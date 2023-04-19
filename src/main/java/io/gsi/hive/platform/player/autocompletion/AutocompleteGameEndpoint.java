package io.gsi.hive.platform.player.autocompletion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**Endpoint for all upstream communications*/
@Service
@ConditionalOnProperty(name = "hive.autocomplete.enabled", havingValue = "true", matchIfMissing = true)
public class AutocompleteGameEndpoint extends ObjectMapperHttpEndpoint {

	private final String GAME_AUTOCOMPLETE_URL = "http://{gameServiceName}/hive/s2s/play/{playId}/autocomplete";

	public AutocompleteGameEndpoint(ObjectMapper objectMapper, @Qualifier("autocompleteRestTemplate") RestTemplate restTemplate) {
		setObjectMapper(objectMapper);
		setRestTemplate(restTemplate);
	}

	public void performAutocomplete(AutocompleteRequest request, String gameServiceName, String playId)
	{
		super.send(
				GAME_AUTOCOMPLETE_URL,
				HttpMethod.POST,
				Optional.of(request),
				Optional.empty(),
				Optional.empty(),
				gameServiceName,
				playId
		);
	}
}
