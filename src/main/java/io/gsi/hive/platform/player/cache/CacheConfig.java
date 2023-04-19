package io.gsi.hive.platform.player.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
	public static final String CACHE_MANAGER_NAME = "cacheManager";
	public static final String TXN_SEARCH_CACHE_NAME = "txnSearchCache";
	public static final String TXN_REPORT_CACHE_NAME = "txnReportCache";
	public static final String PLAY_REPORT_CACHE_NAME = "playReportCache";
	public static final String CLEARDOWN_REPORT_CACHE_NAME = "cleardownReportCache";
	public static final String PLAY_SEARCH_CACHE_NAME = "playSearchCache";
	public static final String GET_PLAY_CACHE_NAME = "getPlayCache";
	public static final String GAME_ID_CACHE_NAME = "gameIdCache";

	public static final String IGP_CODES_CACHE_NAME = "igpCodesCache";
	private final Integer gameIdCacheExpirySeconds;
	private final Integer igpCodesCacheExpirySeconds;

	public CacheConfig(@Value("${endpoint.registry.gameId.cache.expirySeconds:1800}") Integer gameIdCacheExpirySeconds,
					   @Value("${endpoint.registry.igpCodes.cache.expirySeconds:1800}") Integer igpCodesCacheExpirySeconds) {
		this.gameIdCacheExpirySeconds = gameIdCacheExpirySeconds;
		this.igpCodesCacheExpirySeconds = igpCodesCacheExpirySeconds;
	}

	@Bean
	public CacheManager cacheManager() {
		SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
		simpleCacheManager.setCaches(
				Arrays.asList(txnSearchCache(), txnReportCache(), playReportCache(), playSearchCache(),
						cleardownReportCache(), getPlayCache(), gameIdCache(), igpCodesCache()));
		return simpleCacheManager;
	}

	@Bean
	public CaffeineCache txnSearchCache() {
		return new CaffeineCache(TXN_SEARCH_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES)
				.build());
	}

	@Bean
	public CaffeineCache txnReportCache() {
		return new CaffeineCache(TXN_REPORT_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES)
				.build());
	}

	@Bean
	public CaffeineCache playReportCache() {
		return new CaffeineCache(PLAY_REPORT_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES)
				.build());
	}

	@Bean
	public CaffeineCache cleardownReportCache() {
		return new CaffeineCache(CLEARDOWN_REPORT_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES)
				.build());
	}

	@Bean
	public CaffeineCache playSearchCache() {
		return new CaffeineCache(PLAY_SEARCH_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES)
				.build());
	}

	@Bean
	public CaffeineCache getPlayCache() {
		return new CaffeineCache(GET_PLAY_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES)
				.build());
	}

	@Bean
	public CaffeineCache gameIdCache() {
		return new CaffeineCache(GAME_ID_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(gameIdCacheExpirySeconds, TimeUnit.SECONDS)
				.build());
	}

	@Bean
	public CaffeineCache igpCodesCache() {
		return new CaffeineCache(IGP_CODES_CACHE_NAME,
				Caffeine.newBuilder().expireAfterWrite(igpCodesCacheExpirySeconds, TimeUnit.SECONDS)
						.build());
	}
}



