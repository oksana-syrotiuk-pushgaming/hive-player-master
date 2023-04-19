package io.gsi.hive.platform.player.bonus.gateway;

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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BonusWalletEndpointConfig {

	public static final DateFormat APP_DATE_FORMAT = 
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	
	@Value("${endpoint.bonusWallet.http.readTimeout:3000}")
	private int readTimeout;
	
	@Value("${endpoint.bonusWallet.http.connectTimeout:1000}")
	private int connectTimeout;
	
	@Value("${endpoint.bonusWallet.http.maxConnections:100}")
	private int maxConnections;

	@Autowired @Qualifier("bonusWalletEndpointErrorHandler")
	private BonusWalletEndpointErrorHandler endpointErrorHandler;
	
	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	@Bean("bonusWalletClientHttpRequestFactory")
	public ClientHttpRequestFactory bonusWalletClientHttpRequestFactory(){

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

	/**
	 * A non load balanced template for test context
	 */
	@Bean(name="bonusWalletRestTemplate")
	@DependsOn({"bonusWalletClientHttpRequestFactory","bonusWalletEndpointErrorHandler"})
	@ConditionalOnProperty(value = "hive.bonusWallet.endpointLoadBalanced.enabled",havingValue = "false")
	public RestTemplate bonusWalletRestTemplateNonLoadBalanced()
	{
		return bonusWalletRestTemplate();
	}

	@LoadBalanced
	@Bean(name="bonusWalletRestTemplate")
	@DependsOn({"bonusWalletClientHttpRequestFactory","bonusWalletEndpointErrorHandler"})
	@ConditionalOnProperty(value = "hive.bonusWallet.endpointLoadBalanced.enabled",matchIfMissing = true)
	public RestTemplate bonusWalletRestTemplateLoadBalanced()
	{
		return bonusWalletRestTemplate();
	}


	private RestTemplate bonusWalletRestTemplate() {	
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		HttpMessageConverter<?> httpMessageConverter = new StringHttpMessageConverter();
		messageConverters.add(httpMessageConverter);
		
		return restTemplateBuilder
				.requestFactory(this::bonusWalletClientHttpRequestFactory)
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
