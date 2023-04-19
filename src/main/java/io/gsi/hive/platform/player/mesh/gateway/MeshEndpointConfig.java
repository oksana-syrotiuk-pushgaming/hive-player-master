package io.gsi.hive.platform.player.mesh.gateway;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MeshEndpointConfig {

	public static final DateFormat APP_DATE_FORMAT = 
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	
	@Value("${endpoint.mesh.http.readTimeout:3000}")
	private int readTimeout;
	
	@Value("${endpoint.mesh.http.connectTimeout:1000}")
	private int connectTimeout;
	
	@Value("${endpoint.mesh.http.maxConnections:100}")
	private int maxConnections;

	@Autowired @Qualifier("meshEndpointErrorHandler")
	private MeshEndpointErrorHandler endpointErrorHandler;
	
	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	@Bean(name="meshClientHttpRequestFactory")
	public ClientHttpRequestFactory meshClientHttpRequestFactory() {
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		HttpClient httpClient = httpClientBuilder
				.setMaxConnTotal(maxConnections)
				.setMaxConnPerRoute(maxConnections)
				.disableCookieManagement().build();

		HttpComponentsClientHttpRequestFactory clientFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		clientFactory.setReadTimeout(readTimeout);
		clientFactory.setConnectTimeout(connectTimeout);

		return clientFactory;
	}

	@Bean("meshRestTemplate")
	@ConditionalOnProperty(value="endpoint.mesh.loadBalanced.enabled", havingValue = "false")
	@DependsOn({"meshClientHttpRequestFactory","meshEndpointErrorHandler"})
	public RestTemplate gatewayRestTemplate() {
		return meshRestTemplate();
	}

	@Bean("meshRestTemplate")
	@Primary
	@LoadBalanced
	@ConditionalOnProperty(value="endpoint.mesh.loadBalanced.enabled", matchIfMissing = true)
	@DependsOn({"meshClientHttpRequestFactory","meshEndpointErrorHandler"})
	public RestTemplate gatewayRestTemplateLoadBalanced() {
		return meshRestTemplate();
	}

	public RestTemplate meshRestTemplate() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		HttpMessageConverter<?> httpMessageConverter = new StringHttpMessageConverter();
		messageConverters.add(httpMessageConverter);

		return restTemplateBuilder
				.requestFactory(this::meshClientHttpRequestFactory)
				.messageConverters(messageConverters)
				.errorHandler(endpointErrorHandler)
				.build();
	}
	
	public static Registry<ConnectionSocketFactory> getDefaultRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
    }
}
