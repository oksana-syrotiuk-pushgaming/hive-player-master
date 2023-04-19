package io.gsi.hive.platform.player.recon.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.builders.TxnCallbackBuilder;
import io.gsi.hive.platform.player.game.GameBuilder;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.txn.TxnCallback;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class ReconCallbackServiceIT extends ApiITBase {

	private MockRestServiceServer mockRestServiceServer;
	@Autowired private ReconCallbackService callbackService;
	@Autowired @Qualifier("reconRestTemplate") private RestTemplate restTemplate;
	@Autowired private ObjectMapper objectMapper;
	@MockBean private TxnCallbackRepository callbackRepository;
	@MockBean private DiscoveryClient discoveryClient;
	@MockBean private GameService gameService;
	@Autowired
	private CacheManager cacheManager;
	
	@Before
	public void setupMock()
	{			
		this.mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);

		Mockito.when(callbackRepository.findAndLockByTxnId(anyString()))
		.thenAnswer(invocation -> {
			String id = invocation.getArgument(0);
			return TxnCallbackBuilder.txnCallback().withTxnId(id).build();
		});

		Mockito.when(gameService.getGame(anyString())).thenReturn(
				GameBuilder.aGame().build());
		
		Mockito.when(discoveryClient.getServices()).thenReturn(Arrays.asList("hive-game-testGame-service"));
	}

	@Before @After
	public void clearCache() {
		cacheManager.getCache("gameCache").clear();
	}

	@Test
	public void okCallbackIsRoutedToExpectedService() throws JsonProcessingException {

		String gameCode = "reskin";

		Mockito.when(discoveryClient.getServices()).thenReturn(
				Arrays.asList("hive-game-testGame-service", "hive-game-"+gameCode+"-service"));

		TxnCallback callbackTxn = TxnCallbackBuilder.txnCallback()
				.withGameCode(gameCode)
				.withRetries(1)
				.build();

		Mockito.when(gameService.getGame(anyString())).thenReturn(
				GameBuilder.aGame().withCode(gameCode).build());

		Mockito.when(callbackRepository.findAndLockByTxnId(any()))
				.thenReturn(callbackTxn);

		String JsonCallback = 	objectMapper.writeValueAsString(callbackTxn);

		mockRestServiceServer.expect(requestTo("http://hive-game-testGame-service/hive/s2s/txn/1000-1/afterReconciliation"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(content().string(JsonCallback))
				.andRespond(withSuccess());

		callbackService.sendCallback(callbackTxn.getTxnId());

		mockRestServiceServer.verify();

		Mockito.verify(callbackRepository).deleteFromCallbackQueue(callbackTxn.getTxnId());
	}

	@Test
	public void failServiceNotFoundForGameExceedingRetries() throws JsonProcessingException {
		String gameCode = "reskin";

		TxnCallback callbackTxn = TxnCallbackBuilder.txnCallback()
				.withGameCode(gameCode)
				.withRetries(1)
				.build();

		Mockito.when(gameService.getGame(anyString())).thenReturn(
				GameBuilder.aGame().withCode(gameCode)
						.withServiceCode("notFound").build());

		Mockito.when(callbackRepository.findAndLockByTxnId(anyString()))
				.thenReturn(callbackTxn);

		callbackService.sendCallback(callbackTxn.getTxnId());

		Mockito.verify(callbackRepository, never())
				.deleteFromCallbackQueue(anyString());

		Mockito.verify(callbackRepository).incrementRetriesAndSetException(
				callbackTxn.getTxnId(), "InvalidStateException");
	}
	
	@Test
	public void okRouteCallback() throws JsonProcessingException {

		TxnCallback callbackTxn = TxnCallbackBuilder.txnCallback().build();
		String JsonCallback = 	objectMapper.writeValueAsString(callbackTxn);
		
		mockRestServiceServer.expect(requestTo("http://hive-game-testGame-service/hive/s2s/txn/1000-1/afterReconciliation"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(content().string(JsonCallback))
		.andRespond(withSuccess());
		
		callbackService.sendCallback(callbackTxn.getTxnId());
		
		mockRestServiceServer.verify();
		//Should send callback then remove from queue
		Mockito.verify(callbackRepository).deleteFromCallbackQueue(callbackTxn.getTxnId());
	}
		
	@Test
	public void failureUpstreamErrorRetriesIncremented() throws JsonProcessingException
	{
		TxnCallback callbackTxn = TxnCallbackBuilder.txnCallback().build();
		String JsonCallback = objectMapper.writeValueAsString(callbackTxn);
		
		mockRestServiceServer.expect(requestTo("http://hive-game-testGame-service/hive/s2s/txn/1000-1/afterReconciliation"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(content().string(JsonCallback))
		.andRespond(withServerError());
		
		callbackService.sendCallback(callbackTxn.getTxnId());
				
		//Should send callback but not remove from queue, instead increment retries
		Mockito.verify(callbackRepository).incrementRetriesAndSetException(callbackTxn.getTxnId(), "InternalServerError");
		Mockito.verify(callbackRepository, Mockito.times(0)).deleteFromCallbackQueue(any());
	}
}
