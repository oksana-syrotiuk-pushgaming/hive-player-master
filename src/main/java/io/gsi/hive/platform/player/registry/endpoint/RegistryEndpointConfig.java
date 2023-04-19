package io.gsi.hive.platform.player.registry.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RegistryEndpointConfig {
    private final DateFormat APP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private final int readTimeout;
    private final int connectTimeout;
    private final int maxConnections;
    private final RegistryEndpointErrorHandler registryEndpointErrorHandler;

    public RegistryEndpointConfig(
            @Value("${endpoint.registry.http.readTimeout:3000}") int readTimeout,
            @Value("${endpoint.registry.http.connectTimeout:1000}") int connectTimeout,
            @Value("${endpoint.registry.http.maxConnections:10}") int maxConnections,
            RegistryEndpointErrorHandler registryEndpointErrorHandler
    ) {
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.maxConnections = maxConnections;
        this.registryEndpointErrorHandler = registryEndpointErrorHandler;
    }

    @Bean(name="registryClientHttpRequestFactory")
    public HttpComponentsClientHttpRequestFactory registryClientHttpRequestFactory() {
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

    @Bean(name="registryRestTemplate")
    @DependsOn({"registryClientHttpRequestFactory", "registryEndpointErrorHandler"})
    public RestTemplate registryRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(registryClientHttpRequestFactory());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
        objectMapper.setDateFormat(APP_DATE_FORMAT);
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        HttpMessageConverter<?> httpMessageConverter = new StringHttpMessageConverter();
        messageConverters.add(httpMessageConverter);
        restTemplate.setMessageConverters(messageConverters);
        restTemplate.setErrorHandler(registryEndpointErrorHandler);
        return restTemplate;
    }
}
