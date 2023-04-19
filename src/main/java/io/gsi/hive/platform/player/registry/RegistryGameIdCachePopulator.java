package io.gsi.hive.platform.player.registry;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.cache.ParameterRetrievingKey;
import io.gsi.hive.platform.player.registry.gameInfo.ValidGameIds;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(value = "endpoint.registry.gameId.cache.autoRepopulate.enabled", havingValue = "true")
public class RegistryGameIdCachePopulator {
    @Autowired
    private RegistryGateway registryGateway;

    @Autowired
    @Qualifier("gameIdCache")
    private CaffeineCache gameIdCache;

    @EventListener(value = ApplicationReadyEvent.class, condition = "@environment.getActiveProfiles()[0] != 'test'")
    public void onApplicationReadyEvent() {
        populateGameIdCache();
    }

    @Async
    @Scheduled(fixedRateString = "#{(${endpoint.registry.gameId.cache.expirySeconds:1800} * 1000 / 2)}")
    void scheduledGameIdCacheRefresh() {
        refreshGameIdCache();
    }

    private void populateGameIdCache() {
        registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class);
    }

    private void refreshGameIdCache(){
        Map.Entry<?, ?> entry = gameIdCache.getNativeCache()
                .asMap().entrySet().stream()
                .findFirst().orElseThrow(() -> new RuntimeException("gameId cache could not be refreshed"));

        ParameterRetrievingKey parameterRetrievingKey = (ParameterRetrievingKey) entry.getKey();
        Object[] keyParams = parameterRetrievingKey.getParams();
        if (!keyParams[0].equals(RegistryGateway.class) || !keyParams[1].equals("getConfig")) {
            publishGameIdCacheRefreshErrorCounter();
            log.error(new InvalidStateException("gameId cache could not be retrieved").toString());
            throw new InvalidStateException("gameId cache could not be retrieved");
        }
        try {
            Object[] methodParams = (Object[]) keyParams[2];
            Object config = registryGateway.getConfigUncached((String) methodParams[0], (String) methodParams[1], (String) methodParams[2], (Class<?>) methodParams[3]);
            gameIdCache.put(parameterRetrievingKey, config);
        } catch (Exception e) {
            publishGameIdCacheRefreshErrorCounter();
            log.error("gameId cache could not be populated", e);
            gameIdCache.put(parameterRetrievingKey, entry.getValue());
        }
    }

    private void publishGameIdCacheRefreshErrorCounter() {
        Tags tags = Tags.of("error", "gameIdCachePopulateError", "cacheName", "gameId");
        Counter counter = Counter.builder("populate#gameId").tags(tags).register(Metrics.globalRegistry);
        counter.increment();
    }

}
