package io.gsi.hive.platform.player.bonus.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.HivePlayer;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus.Status;
import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.exception.ExceptionResponse;
import io.gsi.hive.platform.player.exception.InsufficientFundsException;
import io.gsi.hive.platform.player.exception.TxnFailedException;
import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
@SpringBootTest(classes={HivePlayer.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties = {"hive.bonusWalletServiceName=hive-bonus-wallet-service-v1"})
public class BonusWalletGatewayIT {

	private MockRestServiceServer mockRestServiceServer;

	@Autowired
	@Qualifier("bonusWalletRestTemplate")
	private RestTemplate restTemplate;

	@Autowired
	private BonusWalletGateway bonusWalletGateway;
	@Autowired
	private ObjectMapper objectMapper;
	@MockBean
	private DiscoveryClient discoveryClient;

	@Value("${hive.bonusWalletServiceName}")
	private String bonusWalletServiceName;
	private String bonusWalletBaseUrl;

	@Before
	public void setupMockRestService() throws Exception {
		bonusWalletBaseUrl = "http://"+ bonusWalletServiceName +"/hive/s2s/platform/bonuswallet/v1";
		this.mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
		Mockito.when(discoveryClient.getServices()).thenReturn(Collections.singletonList(bonusWalletServiceName));
	}

	@Test
	public void okGetWallet() throws JsonProcessingException {

		Wallet wallet = WalletBuilder.aWallet().withFunds(Collections.singletonList(
				FreeroundsFundBuilder.freeroundsFund().build())).build();
		String jsonWallet = objectMapper.writeValueAsString(wallet);

		mockRestServiceServer.expect(requestTo(StringContains.containsString(bonusWalletBaseUrl +"/wallet")))
		.andExpect(method(org.springframework.http.HttpMethod.GET))
		.andExpect(header("BonusWallet-API-Key", "apiKey"))
		.andExpect(queryParam("playerId", PlayerPresets.PLAYERID))
		.andExpect(queryParam("igpCode", IgpPresets.IGPCODE_IGUANA))
		.andExpect(queryParam("gameCode", GamePresets.CODE))
		.andRespond(withSuccess(jsonWallet, MediaType.APPLICATION_JSON_UTF8));

		Wallet receivedWallet = bonusWalletGateway.getWallet(PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA, GamePresets.CODE, PlayerPresets.CCY_CODE );

		assertThat(wallet).isEqualTo(receivedWallet);

		mockRestServiceServer.verify();
	}

	@Test
	public void okProcessTxn() throws JsonProcessingException {

		TxnRequest txnRequest = defaultStakeTxnRequestBuilder().build();
		String jsonTxnRequest = objectMapper.writeValueAsString(txnRequest);

		TxnReceipt receipt = TxnReceiptBuilder.txnReceipt().build();
		String jsonReceipt = objectMapper.writeValueAsString(receipt);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/txn"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(header("BonusWallet-API-Key", "apiKey"))
		.andExpect(content().string(jsonTxnRequest))
		.andRespond(withSuccess(jsonReceipt, MediaType.APPLICATION_JSON_UTF8));

		TxnReceipt receivedReceipt = bonusWalletGateway.processTxn(txnRequest);

		assertThat(receipt).isEqualTo(receivedReceipt);

		mockRestServiceServer.verify();
	}

	@Test
	public void okCancelTxn() throws JsonProcessingException {

		TxnCancelRequest cancelRequest = TxnCancelRequestBuilder.txnCancelRequest().build();
		String jsonCancel = objectMapper.writeValueAsString(cancelRequest);

		TxnReceipt receipt = TxnReceiptBuilder.txnReceipt().build();
		String jsonReceipt = objectMapper.writeValueAsString(receipt);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/txn/1000-1/cancel"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(header("BonusWallet-API-Key", "apiKey"))
		.andExpect(content().string(jsonCancel))
		.andRespond(withSuccess(jsonReceipt, MediaType.APPLICATION_JSON_UTF8));

		TxnReceipt receivedReceipt = bonusWalletGateway.cancelTxn(cancelRequest, TxnPresets.TXNID);

		assertThat(receipt).isEqualTo(receivedReceipt);

		mockRestServiceServer.verify();
	}

	@Test
	public void okCloseFund() {

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/wallet/fund/1/close"))
		.andExpect(method(HttpMethod.POST))
		.andExpect(header("BonusWallet-API-Key", "apiKey"))
		.andRespond(withSuccess());

		bonusWalletGateway.closeFund(WalletPresets.BONUSFUNDID);

		mockRestServiceServer.verify();
	}

	@Test
	public void givenTxnRequest_whenBonusWalletProcessTxn_throwsInsufficientFundsException() throws JsonProcessingException {

		ExceptionResponse exceptionResponse = new ExceptionResponse("InsufficientFunds", "Insufficient funds", "Insufficient funds", "1", null);
		String jsonException = objectMapper.writeValueAsString(exceptionResponse);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/txn"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(header("BonusWallet-API-Key", "apiKey"))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

		assertThatThrownBy(() -> bonusWalletGateway.processTxn(defaultStakeTxnRequestBuilder().build())).isEqualToComparingFieldByField(new InsufficientFundsException("InsufficientFunds"));

		mockRestServiceServer.verify();
	}

	@Test
	public void givenTxnRequest_whenBonusWalletProcessTxn_throwsTxnFailedException() throws JsonProcessingException {

		ExceptionResponse exceptionResponse = new ExceptionResponse("TxnFailed", "Transaction failed", "Transaction failed", "1", null);
		String jsonException = objectMapper.writeValueAsString(exceptionResponse);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/txn"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(header("BonusWallet-API-Key", "apiKey"))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.PRECONDITION_FAILED).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

		assertThatThrownBy(() -> bonusWalletGateway.processTxn(defaultStakeTxnRequestBuilder().build())).isEqualToComparingFieldByField(new TxnFailedException("TxnFailed"));

		mockRestServiceServer.verify();
	}

	@Test
	public void givenTxnRequest_whenBonusWalletProcessTxn_throwsTxnTombstoneException() throws JsonProcessingException {

		ExceptionResponse exceptionResponse = new ExceptionResponse("TxnTombstone", "Transaction tombstoned", "Transaction tombstoned", "1", null);
		String jsonException = objectMapper.writeValueAsString(exceptionResponse);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/txn"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(header("BonusWallet-API-Key", "apiKey"))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.PRECONDITION_FAILED).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

		assertThatThrownBy(() -> bonusWalletGateway.processTxn(defaultStakeTxnRequestBuilder().build())).isEqualToComparingFieldByField(new TxnFailedException("TxnTombstone"));

		mockRestServiceServer.verify();
	}

	@Test
	public void givenTxnRequest_whenBonusWalletProcessTxn_throwsInvalidStateException() throws JsonProcessingException {

		ExceptionResponse exceptionResponse = new ExceptionResponse("InvalidState", "Invalid state", "InvalidStateException", "1", null);
		String jsonException = objectMapper.writeValueAsString(exceptionResponse);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/txn"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(header("BonusWallet-API-Key", "apiKey"))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.CONFLICT).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

		assertThatThrownBy(() -> bonusWalletGateway.processTxn(defaultStakeTxnRequestBuilder().build())).isEqualToComparingFieldByField(new InvalidStateException("Invalid state"));

		mockRestServiceServer.verify();
	}

	@Test
	public void givenTxnRequest_whenBonusWalletProcessTxn_throwsNotFoundException() throws JsonProcessingException {

		ExceptionResponse exceptionResponse = new ExceptionResponse("FundNotFound", "Fund not found", "FundNotFoundException", "1", null);
		String jsonException = objectMapper.writeValueAsString(exceptionResponse);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/txn"))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(header("BonusWallet-API-Key", "apiKey"))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.CONFLICT).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

		assertThatThrownBy(() -> bonusWalletGateway.processTxn(defaultStakeTxnRequestBuilder().build())).isEqualToComparingFieldByField(new NotFoundException("Fund not found"));

		mockRestServiceServer.verify();
	}

	@Test
	public void givenBonusAwardStatus_whenGetBonusAwardStatus_ok() throws JsonProcessingException {

		var awardStatus = FreeRoundsBonusPlayerAwardStatus.builder()
				.status(Status.forfeited)
				.build();

		String jsonReceipt = objectMapper.writeValueAsString(awardStatus);

		mockRestServiceServer.expect(requestTo(bonusWalletBaseUrl +"/igp/"+IgpPresets.IGPCODE_IGUANA +"/fund/"+ WalletPresets.BONUSFUNDID+"/status"))
				.andExpect(method(org.springframework.http.HttpMethod.GET))
				.andExpect(header("BonusWallet-API-Key", "apiKey"))
				.andRespond(withSuccess(jsonReceipt, MediaType.APPLICATION_JSON_UTF8));

		var status = bonusWalletGateway.getBonusAwardStatus(IgpPresets.IGPCODE_IGUANA, WalletPresets.BONUSFUNDID);

		assertThat(status.getStatus()).isEqualTo(Status.forfeited);

		mockRestServiceServer.verify();
	}
}
