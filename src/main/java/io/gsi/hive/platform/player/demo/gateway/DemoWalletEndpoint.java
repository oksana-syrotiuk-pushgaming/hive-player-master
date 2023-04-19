package io.gsi.hive.platform.player.demo.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DemoWalletEndpoint extends ObjectMapperHttpEndpoint {

	public DemoWalletEndpoint(@Qualifier("demoWalletRestTemplate") RestTemplate restTemplate,
							  ObjectMapper objectMapper) {
		setRestTemplate(restTemplate);
		setObjectMapper(objectMapper);
	}
}
