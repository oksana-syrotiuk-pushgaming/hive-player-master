package io.gsi.hive.platform.player.game.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
public class GameCacheConfig {

	@Bean @Primary
	public CacheManager gameCacheManager()
	{
		SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
		simpleCacheManager.setCaches(Arrays.asList(gameListCache()));				
		return simpleCacheManager;
	}

	@Bean
	public CaffeineCache gameListCache()
	{
		//Default to no eviction. Limit of 500 should be well above requirements. for now.
		return new CaffeineCache("gameCache",
				Caffeine.newBuilder()
				.maximumSize(500l)
				.build());
	}
}
