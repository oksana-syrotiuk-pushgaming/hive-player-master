package io.gsi.hive.platform.player.cache;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.registry.RegistryGameIdCachePopulator;
import io.gsi.hive.platform.player.registry.RegistryGateway;
import io.gsi.hive.platform.player.registry.endpoint.RegistryEndpoint;
import io.gsi.hive.platform.player.registry.gameInfo.ValidGameIds;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(value = "/config/test.properties", properties = {"endpoint.registry.gameId.cache.autoRepopulate.enabled=true"})
public class RegistryGameIdCachePopulatorIT {
    private final String REGISTRY_CONFIG_URL = "http://{hiveRegistryServiceName}/hive/s2s/platform/registry/v1/config-merged?type={type}&tagKeys={tagKeys}&tagValues={tagValues}";

    @MockBean
    private RegistryEndpoint registryEndpoint;

    @Autowired
    @Qualifier(CacheConfig.GAME_ID_CACHE_NAME)
    private CaffeineCache gameIdCache;

    @Autowired
    private RegistryGameIdCachePopulator registryGameIdCachePopulator;

    @Value("${endpoint.registry.gameId.cache.expirySeconds}")
    Integer cacheTimeoutSeconds;

    private static final Object[] keyRetrievalParams = {"GAME_ID", "configType", "gameIdConfig", ValidGameIds.class};
    private static final ParameterRetrievingKey parameterRetrievingKey = new ParameterRetrievingKey(RegistryGateway.class, "getConfig", keyRetrievalParams);

    @Before
    public void setup() {
        gameIdCache.clear();
    }

    @Test
    public void givenOkResponseThenInvalidResponseFromRegistry_whenCacheTimeoutSecondsPassed_thenCacheRepopulatedWithOkValue() throws Exception {
        ValidGameIds expected = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000)).build();
        ValidGameIds invalid = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000, "testgame2", 1000)).build();
        doReturn(Optional.of(expected)).doReturn(Optional.of(invalid)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        registryGameIdCachePopulator.onApplicationReadyEvent();
        Thread.sleep(cacheTimeoutSeconds * 1000 + 10);
        Cache.ValueWrapper existingValueWrapper = gameIdCache.get(parameterRetrievingKey);
        assertThat(existingValueWrapper.get()).isEqualTo(expected);
        verify(registryEndpoint, atLeast(2)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
    }

    @Test
    public void givenOkResponseThenEmptyResponseFromRegistry_whenCacheTimeoutSecondsPassed_thenCacheRepopulatedWithOkValue() throws Exception {
        ValidGameIds expected = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000)).build();
        doReturn(Optional.of(expected)).doReturn(Optional.empty()).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        registryGameIdCachePopulator.onApplicationReadyEvent();
        Thread.sleep(cacheTimeoutSeconds * 1000 + 10);
        Cache.ValueWrapper existingValueWrapper = gameIdCache.get(parameterRetrievingKey);
        assertThat(existingValueWrapper.get()).isEqualTo(expected);
        verify(registryEndpoint, atLeast(2)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
    }

    @Test
    public void givenOkResponseFromRegistry_whenCacheTimeoutSecondsPassed_thenCacheRepopulated() throws Exception {
        ValidGameIds expected = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000)).build();
        doReturn(Optional.of(expected)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        registryGameIdCachePopulator.onApplicationReadyEvent();
        Thread.sleep(cacheTimeoutSeconds * 1000 + 10);
        Cache.ValueWrapper existingValueWrapper = gameIdCache.get(parameterRetrievingKey);
        assertThat(existingValueWrapper.get()).isEqualTo(expected);
        verify(registryEndpoint, atLeast(2)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
    }

    @Test
    public void givenOkResponseFromRegistry_whenOnApplicationReadEvent_thenCachePopulated() {
        ValidGameIds expected = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000)).build();
        doReturn(Optional.of(expected)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        registryGameIdCachePopulator.onApplicationReadyEvent();
        Cache.ValueWrapper existingValueWrapper = gameIdCache.get(parameterRetrievingKey);
        assertThat(existingValueWrapper.get()).isEqualTo(expected);
    }

    @Test
    public void givenEmptyResponseFromRegistry_whenOnApplicationReadyEvent_thenNotFoundExceptionThrown() {
        doReturn(Optional.empty()).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        assertThatThrownBy(() -> registryGameIdCachePopulator.onApplicationReadyEvent())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No valid configuration found");
    }

    @Test
    public void givenInvalidResponseFromRegistry_whenOnApplicationReadyEvent_thenExceptionThrown() {
        ValidGameIds invalid = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000, "testgame2", 1000)).build();
        doReturn(Optional.of(invalid)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        assertThatThrownBy(() -> registryGameIdCachePopulator.onApplicationReadyEvent())
                .isInstanceOf(InternalServerException.class)
                .hasMessage("constraint violations: gameCodeToGameId: validator.gameIds.invalid");
    }

    @Test
    public void givenCachePopulator_thenOnApplicationReadyEventMethodHasCorrectAnnotation() throws Exception {
        EventListener annotation = RegistryGameIdCachePopulator.class.getDeclaredMethod("onApplicationReadyEvent").getAnnotation(EventListener.class);
        assertThat(annotation.value().length).isOne();
        assertThat(annotation.value()[0]).isEqualTo(ApplicationReadyEvent.class);
        assertThat(annotation.condition()).isEqualTo("@environment.getActiveProfiles()[0] != 'test'");
    }

    @Test
    public void givenCachePopulator_thenScheduledConfigCacheRefreshHasCorrectAnnotations() throws Exception {
        Method method = RegistryGameIdCachePopulator.class.getDeclaredMethod("scheduledGameIdCacheRefresh");
        Async asyncAnnotation = method.getAnnotation(Async.class);
        assertThat(asyncAnnotation).isNotNull();
        Scheduled scheduledAnnotation = method.getAnnotation(Scheduled.class);
        assertThat(scheduledAnnotation.fixedRateString()).isEqualTo("#{(${endpoint.registry.gameId.cache.expirySeconds:1800} * 1000 / 2)}");
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("RegistryService-API-Key", "registryTestApiKey");
        return httpHeaders;
    }
}
