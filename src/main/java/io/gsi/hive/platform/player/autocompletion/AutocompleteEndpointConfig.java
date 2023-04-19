/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.autocompletion;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * AutocompleteEndpointConfig
 *
 */
@Configuration
@Import({io.gsi.commons.config.MetricsConfig.class})
@EnableScheduling
@ConditionalOnProperty(name = "hive.autocomplete.enabled", havingValue = "true", matchIfMissing = true)
class AutocompleteEndpointConfig {

	@Value("${hive.autocomplete.http.readTimeout:3000}")
	private int readTimeout;

	@Value("${hive.autocomplete.http.connectTimeout:1000}")
	private int connectTimeout;

	@Value("${hive.autocomplete.http.maxConnections:100}")
	private int maxConnections;

	@Bean
	ThreadPoolTaskExecutor autocompleteTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("atc-worker-");
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setKeepAliveSeconds(300);
		return taskExecutor;
	}

	@Bean
	ThreadPoolTaskExecutor autocompleteRequestTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("atc-request-worker-");
		taskExecutor.setCorePoolSize(15);
		taskExecutor.setMaxPoolSize(30);
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setKeepAliveSeconds(100);
		return taskExecutor;
	}

	//Needed for Testing
	@Bean("autocompleteRestTemplate")
	@ConditionalOnProperty(value = "hive.autocomplete.endpointLoadBalanced.enabled",havingValue = "false")
	public RestTemplate autocompleteRestTemplate() {
		return restTemplate();
	}

	@LoadBalanced
	@Bean("autocompleteRestTemplate")
	@ConditionalOnProperty(value = "hive.autocomplete.endpointLoadBalanced.enabled",matchIfMissing = true)
	public RestTemplate autocompleteRestTemplateLoadBalanced() {
		return restTemplate();
	}

	private RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		HttpMessageConverter<String> httpMessageConverter =
				new StringHttpMessageConverter();
		messageConverters.add(httpMessageConverter);
		restTemplate.setMessageConverters(messageConverters);
		return restTemplate;
	}

	@Bean("autoCompleteEndpointRequestFactory")
	public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
		HttpClient httpClient = HttpClientBuilder.create()
				.setMaxConnTotal(maxConnections)
				.setMaxConnPerRoute(maxConnections)
				.disableCookieManagement().build();
		HttpComponentsClientHttpRequestFactory clientFactory =
				new HttpComponentsClientHttpRequestFactory(httpClient);
		clientFactory.setReadTimeout(readTimeout);
		clientFactory.setConnectTimeout(connectTimeout);
		return clientFactory;
	}
}
