package io.gsi.hive.platform.player.recon.game;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnRequest;

public class ReconGamePollingServiceIT extends ApiITBase {

	private MockRestServiceServer mockRestServiceServer;
	@Autowired
	private ReconGamePollingService reconGamePollingService;

	@Autowired @Qualifier("reconRestTemplate")
	private RestTemplate restTemplate;
	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private DiscoveryClient discoveryClient;

	@MockBean
	private ReconTxnIntegrationService reconTxnIntegrationService;
	
	@Before
	public void setupMock()
	{			
		this.mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
		
		when(discoveryClient.getServices()).thenReturn(Arrays.asList("hive-game-heavenandhell", "hive-game-jdean"));
	}

	@Test
	public void noPendingTxnsOk() {

		mockRestServiceServer.expect(requestTo("http://hive-game-heavenandhell/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(GET))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		mockRestServiceServer.expect(requestTo("http://hive-game-jdean/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(GET))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		reconGamePollingService.fetchAndIntegratePendingGameTxnEvents();
	}

	@Test
	public void oneGameCallbackFailureDoesntStopAnother() {

		mockRestServiceServer.expect(requestTo("http://hive-game-heavenandhell/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(GET))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		mockRestServiceServer.expect(requestTo("http://hive-game-jdean/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(GET))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		reconGamePollingService.fetchAndIntegratePendingGameTxnEvents();
	}

	@Test
	public void singlePendingTxnRequestFromOneGame() throws JsonProcessingException {
		TxnRequest txnRequest = defaultStakeTxnRequestBuilder().build();
		String txnRequestStr = objectMapper.writeValueAsString(Arrays.asList(txnRequest));

		mockRestServiceServer.expect(requestTo("http://hive-game-heavenandhell/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(GET))
				.andRespond(withSuccess(txnRequestStr,MediaType.APPLICATION_JSON));

		mockRestServiceServer.expect(requestTo("http://hive-game-jdean/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(GET))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		reconGamePollingService.fetchAndIntegratePendingGameTxnEvents();

		verify(reconTxnIntegrationService).integrateGameTxn(txnRequest);
	}

	@Test
	public void singlePendingTxnCancelFromOneGame() throws JsonProcessingException {
		TxnCancelRequest txnCancelRequest = TxnCancelRequestBuilder.txnCancelRequest().build();
		String txnCancelRequestStr = objectMapper.writeValueAsString(Arrays.asList(txnCancelRequest));

		mockRestServiceServer.expect(requestTo("http://hive-game-heavenandhell/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(org.springframework.http.HttpMethod.GET))
				.andRespond(withSuccess(txnCancelRequestStr,MediaType.APPLICATION_JSON));

		mockRestServiceServer.expect(requestTo("http://hive-game-jdean/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(org.springframework.http.HttpMethod.GET))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		reconGamePollingService.fetchAndIntegratePendingGameTxnEvents();

		verify(reconTxnIntegrationService).integrateCancelTxn(txnCancelRequest);
	}

	@Test
	public void txnCancelRequestFromOneGameTxnRequestFromAnother() throws JsonProcessingException {
		TxnRequest txnRequest = defaultStakeTxnRequestBuilder().build();
		String txnRequestStr = objectMapper.writeValueAsString(Arrays.asList(txnRequest));
		TxnCancelRequest txnCancelRequest = TxnCancelRequestBuilder.txnCancelRequest().build();
		String txnCancelRequestStr = objectMapper.writeValueAsString(Arrays.asList(txnCancelRequest));

		mockRestServiceServer.expect(requestTo("http://hive-game-heavenandhell/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(org.springframework.http.HttpMethod.GET))
				.andRespond(withSuccess(txnCancelRequestStr,MediaType.APPLICATION_JSON));

		mockRestServiceServer.expect(requestTo("http://hive-game-jdean/hive/s2s/pendingtxnevents?limit=200&minAgeMinutes=2"))
				.andExpect(method(org.springframework.http.HttpMethod.GET))
				.andRespond(withSuccess(txnRequestStr, MediaType.APPLICATION_JSON));

		reconGamePollingService.fetchAndIntegratePendingGameTxnEvents();

		verify(reconTxnIntegrationService).integrateCancelTxn(txnCancelRequest);
		verify(reconTxnIntegrationService).integrateGameTxn(txnRequest);
	}
}
