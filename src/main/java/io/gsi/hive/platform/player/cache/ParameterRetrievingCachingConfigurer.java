package io.gsi.hive.platform.player.cache;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParameterRetrievingCachingConfigurer extends CachingConfigurerSupport {
    @Override
    public KeyGenerator keyGenerator() {
        return new ParameterRetrievingKeyGenerator();
    }
}
