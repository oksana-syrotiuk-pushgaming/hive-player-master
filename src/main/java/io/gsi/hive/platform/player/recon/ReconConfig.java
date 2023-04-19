/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.recon;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

/**
 * ReconConfig
 *
 */
@Configuration
@Import({io.gsi.commons.config.MetricsConfig.class})
@EnableScheduling
@ConditionalOnProperty(name = "hive.recon.enabled", havingValue = "true", matchIfMissing = true)
class ReconConfig {

	@Value("${hive.recon.http.readTimeout:3000}")
	private int readTimeout;
	
	@Value("${hive.recon.http.connectTimeout:1000}")
	private int connectTimeout;
	
	@Value("${hive.recon.http.maxConnections:100}")
	private int maxConnections;
	
	@Bean
	ThreadPoolTaskExecutor reconTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("rcn-worker-");
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setKeepAliveSeconds(300);
		return taskExecutor;
	}
	
	@Bean
	ThreadPoolTaskExecutor callbackTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("rcn-callback-worker-");
		taskExecutor.setCorePoolSize(15);
		taskExecutor.setMaxPoolSize(30);
		taskExecutor.setAllowCoreThreadTimeOut(true);
		taskExecutor.setKeepAliveSeconds(100);
		return taskExecutor;
	}

	//Needed for Testing
	@Bean("reconRestTemplate")
	@ConditionalOnProperty(value = "hive.recon.endpointLoadBalanced.enabled",havingValue = "false")
	public RestTemplate reconRestTemplate() {
		return restTemplate();
	}
	
	@LoadBalanced
	@Bean("reconRestTemplate")
	@ConditionalOnProperty(value = "hive.recon.endpointLoadBalanced.enabled",matchIfMissing = true)
	public RestTemplate reconRestTemplateLoadBalanced() {
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
	
	@Bean("reconRestTemplateRequestFactory")
	public ClientHttpRequestFactory clientHttpRequestFactory() {
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
