package io.gsi.hive.platform.player.mesh.igpservicelocator;

import io.gsi.commons.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "hive.igpService.locator", havingValue = "default", matchIfMissing = true)
public class IgpServiceLocationCache {
	private Integer repopulatingInterval;
	private boolean populateOnStartup;

	private final AtomicInteger currentRepopulatingInterval;
	private final IgpServiceLocationCachePopulator igpServiceLocationCachePopulator;

	//Map<igpCode, igpServiceCode> - igpServiceCode is the base igp code of the service that serves this igpCode
	private final Map<String, String> igpServiceCodesCache;

	@Autowired
	public IgpServiceLocationCache(IgpServiceLocationCachePopulator igpServiceLocationCachePopulator,
								   @Value("${hive.igpService.locator.cache.repopulation.onStartup:true}") boolean populateOnStartup,
								   @Value("${hive.igpService.locator.cache.repopulation.invalidateAfterRepopulations:50}") Integer repopulatingInterval) {
		this.igpServiceCodesCache = new HashMap<>();
		this.igpServiceLocationCachePopulator = igpServiceLocationCachePopulator;
		this.currentRepopulatingInterval = new AtomicInteger();
		this.populateOnStartup = populateOnStartup;
		this.repopulatingInterval = repopulatingInterval;
	}

	@EventListener(ApplicationReadyEvent.class)
	private void populateOnStartup() {
		if(populateOnStartup) {
			populateIgpServiceCodes();
		}
	}

	//By default run every 10 mins, after 1 min
	@Scheduled(fixedDelayString = "${hive.igpService.locator.cache.repopulation.intervalMillis:600000}",
			initialDelayString = "${hive.igpService.locator.cache.repopulation.initialDelayMillis:60000}")
	public void populateIgpServiceCodes() {
		Thread.currentThread().setName("igp-service-location-cache");
		final var igpServiceCodes = igpServiceLocationCachePopulator.getIgpServiceCodes();
		checkForRepopulation(igpServiceCodes);
		igpServiceCodesCache.putAll(igpServiceCodes);
		log.debug("IgpCodes cache updated");
	}

	public String getServiceCode(String igpCode) {
		if (!igpServiceCodesCache.containsKey(igpCode)) {
			throw new NotFoundException(
					format("service location for igpCode=%s not found", igpCode));
		}
		return igpServiceCodesCache.get(igpCode);
	}

	private void clear(Map<String,String> igpServiceCodesToRetain) {
		/* Instead of fully clearing the cache (igpServiceCodesCache), only igp codes which are no longer
		 present in the latest list (igpServiceCodesToRetain) are removed from the cache */
		igpServiceCodesCache.entrySet().retainAll(igpServiceCodesToRetain.entrySet());
	}

	private void checkForRepopulation(Map<String,String> igpServiceCodes) {
		if (currentRepopulatingInterval.decrementAndGet() <= 0) {
			currentRepopulatingInterval.set(repopulatingInterval);
			clear(igpServiceCodes);
		}
	}
}
