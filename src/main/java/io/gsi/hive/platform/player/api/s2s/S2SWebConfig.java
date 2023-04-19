package io.gsi.hive.platform.player.api.s2s;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class S2SWebConfig implements WebMvcConfigurer {

	@Autowired
	private HivePlayerAuthInterceptor externalAuthInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(externalAuthInterceptor)
				.addPathPatterns("/s2s/platform/player/v1/**");
	}
}
