package io.gsi.hive.platform.player.recon.game;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpoint;
import io.gsi.hive.platform.player.txn.TxnCallback;
import io.gsi.hive.platform.player.txn.event.TxnEvent;

/**Endpoint for all upstream communications*/
@Service
@ConditionalOnProperty(name = "hive.recon.enabled", havingValue = "true", matchIfMissing = true)
public class ReconGameEndpoint extends ObjectMapperHttpEndpoint{

	@Autowired
	public ReconGameEndpoint( ObjectMapper objectMapper, @Qualifier("reconRestTemplate") RestTemplate restTemplate) {
		setObjectMapper(objectMapper);
		setRestTemplate(restTemplate);
	}

	//TODO upgrade the ObjectMapperHttpEndpoint to handle TypeReferences in addition to basic types so that it can
	//handle these kinds of jobs
	public List<TxnEvent> getUpstreamPendingTxns(String url)
	{
		ResponseEntity<String> responseEntity;
		try {
			responseEntity = restTemplate.exchange(url,HttpMethod.GET,null,String.class);
		} catch (HttpMessageConversionException e) {
			throw new RuntimeException(e.getMessage());
		}
		String responseBody = responseEntity.getBody();
		try {
			return this.objectMapper.readerFor(new TypeReference<List<TxnEvent>>() {}).readValue(responseBody);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void sendCallback(String url, TxnCallback callbackTxn)
	{
		super.send(url, HttpMethod.POST, Optional.of(callbackTxn), Optional.empty());
	}
}
