package io.gsi.hive.platform.player.api.bo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@ConditionalOnProperty(value="hive.player.backOffice.apiKey.enabled",
    havingValue = "true")
@Configuration
public class BoWebConfig implements WebMvcConfigurer {

    @Autowired
    private BoApiKeyInterceptor boAPIKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(boAPIKeyInterceptor)
            .addPathPatterns("/bo/platform/player/v1/**");
    }

}
