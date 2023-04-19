package io.gsi.hive.platform.player.bonus.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BonusWalletEndpoint extends ObjectMapperHttpEndpoint {


	public BonusWalletEndpoint(@Qualifier("bonusWalletRestTemplate") RestTemplate restTemplate,
							   ObjectMapper objectMapper) {
		setRestTemplate(restTemplate);
		setObjectMapper(objectMapper);
	}
}
