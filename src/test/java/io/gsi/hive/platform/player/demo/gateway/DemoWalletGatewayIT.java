package io.gsi.hive.platform.player.demo.gateway;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.gsi.hive.platform.player.HivePlayer;
import io.gsi.hive.platform.player.builders.GuestLoginBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.demo.builders.GuestPlayerBuilder;
import io.gsi.hive.platform.player.demo.builders.GuestWalletBuilder;
import io.gsi.hive.platform.player.demo.builders.GuestWalletCreateBuilder;
import io.gsi.hive.platform.player.demo.player.GuestPlayer;
import io.gsi.hive.platform.player.demo.wallet.GuestWallet;
import io.gsi.hive.platform.player.demo.wallet.GuestWalletCreate;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;

@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
@SpringBootTest(classes={HivePlayer.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties={"hive.demoWalletServiceName=hive-demo-wallet-service-v1"})
public class DemoWalletGatewayIT {

	private MockRestServiceServer mockRestServiceServer;

	@Autowired
	@Qualifier("demoWalletRestTemplate")
	private RestTemplate restTemplate;
	@Autowired
	private DemoWalletGateway demoWalletGateway;
	@Autowired
	private ObjectMapper objectMapper;

	@Value("${hive.demoWalletServiceName}")
	private String demoWalletServiceName;
	private String demoWalletBaseUrl;

	@Before
	public void setupMockRestService() throws Exception {
		demoWalletBaseUrl = "http://"+ demoWalletServiceName +"/hive/s2s/platform/demowallet/v1";
		this.mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
	}

	@Test
	public void okCreateGuestPlayer() throws JsonProcessingException {

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().build();
		String jsonGuestLogin = objectMapper.writeValueAsString(guestLogin);

		GuestPlayer player = GuestPlayerBuilder.aPlayer().build();
		String JsonPlayer = 	objectMapper.writeValueAsString(player);

		mockRestServiceServer.expect(requestTo(demoWalletBaseUrl +"/player"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(header("DemoWallet-API-Key", "apiKey"))
		.andExpect(content().string(jsonGuestLogin))
		.andRespond(withSuccess(JsonPlayer, MediaType.APPLICATION_JSON_UTF8));

		GuestPlayer receivedPlayer = demoWalletGateway.createGuestPlayer(guestLogin);

		assertThat(player).isEqualTo(receivedPlayer);

		mockRestServiceServer.verify();
	}

	@Test
	public void okCreateWallet() throws JsonProcessingException {

		GuestWalletCreate guestWalletCreate = GuestWalletCreateBuilder.guestWalletCreate().build();
		String jsonGuestWalletCreate = objectMapper.writeValueAsString(guestWalletCreate);

		GuestWallet wallet = GuestWalletBuilder.guestWallet().build();
		String jsonWallet = objectMapper.writeValueAsString(wallet);

		mockRestServiceServer.expect(requestTo(demoWalletBaseUrl +"/wallet"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(header("DemoWallet-API-Key", "apiKey"))
		.andExpect(content().string(jsonGuestWalletCreate))
		.andRespond(withSuccess(jsonWallet, MediaType.APPLICATION_JSON_UTF8));

		GuestWallet receivedWallet = demoWalletGateway.createWallet(guestWalletCreate);

		assertThat(wallet).isEqualTo(receivedWallet);

		mockRestServiceServer.verify();
	}

	@Test
	public void okGetWallet() throws JsonProcessingException {

		GuestWallet wallet = GuestWalletBuilder.guestWallet().build();
		String jsonWallet = objectMapper.writeValueAsString(wallet);

		mockRestServiceServer.expect(requestTo(StringContains.containsString(demoWalletBaseUrl +"/wallet")))
		.andExpect(method(org.springframework.http.HttpMethod.GET))
		.andExpect(header("DemoWallet-API-Key", "apiKey"))
		.andExpect(queryParam("playerId", PlayerPresets.PLAYERID))
		.andExpect(queryParam("igpCode", IgpPresets.IGPCODE_IGUANA))
		.andExpect(queryParam("gameCode", GamePresets.CODE))
		.andRespond(withSuccess(jsonWallet, MediaType.APPLICATION_JSON_UTF8));

		GuestWallet receivedWallet = demoWalletGateway.getWallet(PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA, GamePresets.CODE);

		assertThat(wallet).isEqualTo(receivedWallet);

		mockRestServiceServer.verify();
	}

	@Test
	public void okProcessTxn() throws JsonProcessingException {

		TxnRequest txnRequest = defaultStakeTxnRequestBuilder().build();
		String jsonTxnRequest = objectMapper.writeValueAsString(txnRequest);

		TxnReceipt receipt = TxnReceiptBuilder.txnReceipt().build();
		String jsonReceipt = objectMapper.writeValueAsString(receipt);

		mockRestServiceServer.expect(requestTo(demoWalletBaseUrl +"/txn"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(header("DemoWallet-API-Key", "apiKey"))
		.andExpect(content().string(jsonTxnRequest))
		.andRespond(withSuccess(jsonReceipt, MediaType.APPLICATION_JSON_UTF8));

		TxnReceipt receivedReceipt = demoWalletGateway.processTxn(txnRequest);

		assertThat(receipt).isEqualTo(receivedReceipt);

		mockRestServiceServer.verify();
	}

	@Test
	public void okCancelTxn() throws JsonProcessingException {

		TxnCancelRequest cancelRequest = TxnCancelRequestBuilder.txnCancelRequest().build();
		String jsonCancel = objectMapper.writeValueAsString(cancelRequest);

		TxnReceipt receipt = TxnReceiptBuilder.txnReceipt().build();
		String jsonReceipt = objectMapper.writeValueAsString(receipt);

		mockRestServiceServer.expect(requestTo(demoWalletBaseUrl +"/txn/1000-1/cancel"))
		.andExpect(method(org.springframework.http.HttpMethod.POST))
		.andExpect(header("DemoWallet-API-Key", "apiKey"))
		.andExpect(content().string(jsonCancel))
		.andRespond(withSuccess(jsonReceipt, MediaType.APPLICATION_JSON_UTF8));

		TxnReceipt receivedReceipt = demoWalletGateway.cancelTxn(cancelRequest, TxnPresets.TXNID);

		assertThat(receipt).isEqualTo(receivedReceipt);

		mockRestServiceServer.verify();
	}
}
