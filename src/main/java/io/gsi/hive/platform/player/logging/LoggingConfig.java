/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.logging;

import io.gsi.commons.config.HttpLoggingConfig;
import io.gsi.commons.http.response.ResponseLoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Optional;

@Configuration
@Import({
	io.gsi.commons.config.LoggingConfig.class,
	HttpLoggingConfig.class
})
public class LoggingConfig {

	private final ResponseLoggingFilter responseLoggingFilter;
	private final ResponseLoggingFilterConfigurer responseLoggingFilterConfigurer;

	public LoggingConfig(@Lazy @Autowired(required = false)
								 ResponseLoggingFilterConfigurer responseLoggingFilterConfigurer,
			ResponseLoggingFilter responseLoggingFilter) {
		this.responseLoggingFilterConfigurer = responseLoggingFilterConfigurer;
		this.responseLoggingFilter = responseLoggingFilter;
	}

	@Bean @ConditionalOnMissingBean(value=ResponseLoggingFilterConfigurer.class)
	public ResponseLoggingFilterConfigurer defaultResponseLoggingFilterConfigurer() {
		return new ResponseLoggingFilterConfigurer() {
			@Override
			public void configure(ResponseLoggingFilter filter) {
				filter.setIncludeUrlPathPatterns(Arrays.asList("/s2s/.*"));
			}
		};
	}

	@PostConstruct
	public void configureFilter() {
		Optional.ofNullable(responseLoggingFilterConfigurer).ifPresent(c -> c.configure(responseLoggingFilter));
	}

}
