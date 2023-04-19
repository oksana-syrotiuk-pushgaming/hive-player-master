package io.gsi.hive.platform.player.api.s2s;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.builders.SessionBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.exception.*;
import io.gsi.hive.platform.player.game.GameBuilder;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.recon.ManualReconService;
import io.gsi.hive.platform.player.registry.gameInfo.GameIdService;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.session.SessionStatus;
import io.gsi.hive.platform.player.txn.BonusFundDetailsPresets;
import io.gsi.hive.platform.player.txn.TxnService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.*;

/*Txn Service is IT's separately, so this just covers the API and Exceptions*/
public class S2STxnControllerIT extends ApiITBase{

	@MockBean
	private TxnService txnService;
	@MockBean
	private SessionService sessionService;
	@MockBean
	private GameService gameService;
	@MockBean
	private GameIdService gameIdService;
	@MockBean
	private ManualReconService manualReconService;

	@Autowired
	private HivePlayerAuthInterceptor authInterceptor;

	@Before
	public void setup() {
		Session session = new Session();
		session.setPlayerId(PlayerPresets.PLAYERID);
		session.setId(SessionPresets.SESSIONID);
		session.setGameCode(GamePresets.CODE);
		Mockito.when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
		Mockito.when(gameService.getGame(GamePresets.CODE)).thenReturn(GameBuilder.aGame().build());
		Mockito.doNothing().when(gameIdService).validateGameId(any(), any());
	}

	@Test
	public void givenOldFormatIds_whenProcessTxn_thenOk() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();
		TxnReceipt receipt = TxnReceiptBuilder.txnReceipt().build();

		Mockito.when(txnService.process(any()))
		.thenReturn(receipt);

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(200)
		.body("txnId", equalTo("1000-1"))
		.body("gameCode", equalTo(GamePresets.CODE));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void givenNewFormatIds_whenProcessTxn_thenOk()
	{
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnId("1000-9999-1")
				.playId("1000-9999-10")
				.roundId("1000-9999-10")
				.build();
		TxnReceipt receipt = TxnReceiptBuilder.txnReceipt().withTxnId("1000-9999-1").build();

		Mockito.when(txnService.process(any()))
				.thenReturn(receipt);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(200)
				.body("txnId", equalTo("1000-9999-1"))
				.body("gameCode", equalTo(GamePresets.CODE));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void processTxnWithExtraInfo()
	{
		Map<String, Object> extraInfo = new LinkedHashMap<>();
		extraInfo.put("freespins",true);
		TxnRequest request = defaultStakeTxnRequestBuilder().extraInfo(extraInfo).build();
		TxnReceipt reciept = TxnReceiptBuilder.txnReceipt().build();

		Mockito.when(txnService.process(any()))
				.thenReturn(reciept);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(200)
				.body("txnId", equalTo("1000-1"))
				.body("gameCode", equalTo(GamePresets.CODE));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void processTxnExceptionWithExtraInfo() {
		final var extraInfo = Map.of("Name", (Object)"Value");
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.extraInfo(extraInfo)
				.build();

		Mockito.when(txnService.process(any()))
				.thenThrow(new ApiAuthorizationException("BadUser", extraInfo));

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(401)
				.body("extraInfo.Name", equalTo("Value"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void processUnexpectedTxnExceptionWithExtraInfo() {
		final var extraInfo = Map.of("Name", (Object)"Value");
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.extraInfo(extraInfo)
				.build();

		Mockito.when(txnService.process(any()))
				.thenThrow(new ApiUnexpectedException("OtherError", extraInfo));

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(500)
				.body("extraInfo.Name", equalTo("Value"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void processTxnWithOperatorFreeroundsFund() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultOperatorBonusFundDetails().build())
				.build();
		TxnReceipt receipt = TxnReceiptBuilder.txnReceipt()
				.withWallet(WalletPresets.walletWithOperatorFreeroundsFund())
				.build();

		Mockito.when(txnService.process(request))
				.thenReturn(receipt);

		TxnReceipt txnReceipt = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(200)
				.extract()
				.body()
				.as(TxnReceipt.class);
		assertThat(txnReceipt).isEqualTo(receipt);

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failProcessTxnNoApiKey() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		RestAssured.given()
		.log().all()
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(401)
		.body("code", equalTo("Authorization"))
		.body("msg", equalTo("No Hive API Key supplied in header"));
	}

	@Test
	public void okProcessTxnApiKeyOff()
	{
		authInterceptor.setApiKeyEnabled(false);
		TxnRequest request = defaultStakeTxnRequestBuilder().build();
		TxnReceipt reciept = TxnReceiptBuilder.txnReceipt().build();

		Mockito.when(txnService.process(any()))
		.thenReturn(reciept);

		RestAssured.given()
		.log().all()
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(200)
		.body("txnId", equalTo("1000-1"))
		.body("gameCode", equalTo(GamePresets.CODE));

		Mockito.verify(txnService).process(request);

		authInterceptor.setApiKeyEnabled(true);
	}

	@Test
	public void processStakeWithNoSession() {
		Mockito.when(sessionService.getSession(any())).thenThrow(new SessionNotFoundException("session not found"));
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnType(TxnType.STAKE)
				.sessionId(null)
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(404)
				.body("code", equalTo("SessionNotFound"))
				.body("msg", equalTo("session not found"));
	}

	@Test
	public void processWinWithNoSession() {
		Mockito.when(sessionService.getSession(Mockito.anyString())).thenThrow(SessionNotFoundException.class);
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnType(TxnType.WIN)
				.sessionId(null)
				.build();
		TxnReceipt reciept = TxnReceiptBuilder.txnReceipt().build();

		Mockito.when(txnService.process(any()))
				.thenReturn(reciept);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(200)
				.body("txnId", equalTo("1000-1"))
				.body("gameCode", equalTo(GamePresets.CODE));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void processStakeWithExpiredSession() {
		Session expiredSession = SessionBuilder.aSession().withLastAccessedTime(1L).build();
		Mockito.when(sessionService.getSession(Mockito.anyString())).thenReturn(expiredSession);
		Mockito.when(sessionService.isExpired(expiredSession)).thenReturn(true);

		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnType(TxnType.STAKE)
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(401)
				.body("code", equalTo("Authorization"))
				.body("msg", equalTo("session expired"));
	}

	@Test
	public void processWinWithExpiredSession()
	{
		Session expiredSession = SessionBuilder.aSession().withLastAccessedTime(1L).build();
		Mockito.when(sessionService.getSession(Mockito.anyString())).thenReturn(expiredSession);
		Mockito.when(sessionService.isExpired(expiredSession)).thenReturn(true);

		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnType(TxnType.WIN)
				.build();
		TxnReceipt reciept = TxnReceiptBuilder.txnReceipt().build();

		Mockito.when(txnService.process(any()))
				.thenReturn(reciept);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(200)
				.body("txnId", equalTo("1000-1"))
				.body("gameCode", equalTo(GamePresets.CODE));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void okProcessTxnInvalid() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.amount(new BigDecimal(-20))
				.build();

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(400)
		.body("code", equalTo("BadRequest"));

		Mockito.verify(txnService, Mockito.times(0)).process(request);
	}

	@Test
	public void failProcessTxnWithPlayCompleteStake() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.playComplete(true)
				.build();

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(409)
		.body("code", equalTo("InvalidState"));

		Mockito.verify(txnService, Mockito.times(0)).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamApiKnownException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new ApiKnownException("API_INSUFFICIENT_FUNDS","message"));

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(409)
		.body("code", equalTo("API_INSUFFICIENT_FUNDS"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamPlayerLimitException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new PlayerLimitException("PlayerLimit"));

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(403)
		.body("code", equalTo("PlayerLimit"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamInsufficientFundsException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new InsufficientFundsException("InsufficientFunds"));

		RestAssured.given()
		.log().all()
		.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
		.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(412)
		.body("code", equalTo("InsufficientFunds"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamUnsupportedOperationException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new UnsupportedOperationException("Not Supported"));

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(501)
		.body("code", equalTo("UnsupportedOperation"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamNotFoundException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new NotFoundException("Not Found"));

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(404)
		.body("code", equalTo("NotFound"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamBadRequestException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new BadRequestException("Bad"));

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(400)
		.body("code", equalTo("BadRequest"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamResourceAccessException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new ResourceAccessException("Reasource"));

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(500)
		.body("code", equalTo("ResourceAccess"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void failureProcessTxnUpstreamUnhandledException() {
		TxnRequest request = defaultStakeTxnRequestBuilder().build();

		Mockito.when(txnService.process(any()))
		.thenThrow(new ClassCastException("No fun exceptions on classpath"));

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn")
		.then()
		.log().all()
		.statusCode(500)
		.body("code", equalTo("InternalServer"));

		Mockito.verify(txnService).process(request);
	}

	@Test
	public void okCancelTxn()
	{
		TxnCancelRequest request = TxnCancelRequestBuilder.txnCancelRequest().build();
		TxnReceipt reciept = TxnReceiptBuilder.txnReceipt().withStatus(TxnStatus.CANCELLED).build();

		Mockito.when(txnService.externalCancel(any()))
		.thenReturn(reciept);

		RestAssured.given()
		.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
		.accept(ContentType.JSON)
		.body(request)
		.post("/hive/s2s/platform/player/v1/txn/cancel")
		.then()
		.log().all()
		.statusCode(200)
		.body("txnId", equalTo("1000-1"))
		.body("gameCode", equalTo(GamePresets.CODE));

		Mockito.verify(txnService).externalCancel(request);
	}

	@Test
	public void givenInternalPlayId_whenProcessTxn_thenThrowException() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.playId("10")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"));
	}

	@Test
	public void givenInternalRoundId_whenProcessTxn_thenThrowException() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.roundId("10")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"));
	}

	@Test
	public void givenInternalTxnId_whenProcessTxn_thenThrowException() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnId("1")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"));
	}

	@Test
	public void givenPlayIdWithMismatchedPlatformId_whenProcessTxn_thenThrowException() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.playId("1000-9998-10")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"))
				.body("msg", equalTo("playId platform identifier does not match injected value."));
	}
	@Test
	public void givenRoundIdWithMismatchedPlatformId_whenProcessTxn_thenThrowException() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.roundId("1000-9998-10")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"))
				.body("msg", equalTo("roundId platform identifier does not match injected value."));
	}

	@Test
	public void givenTxnIdWithMismatchedPlatformId_whenProcessTxn_thenThrowException() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnId("1000-9998-1")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"))
				.body("msg", equalTo("txnId platform identifier does not match injected value."));
	}

	@Test
	public void failureProcessTxnMismatchedGameIdWithCurrentGameSession() {
		//Mock Session configured to return "testGame" as the gameCode inside the session.
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.gameCode("notATestGame")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"));
	}

	@Test
	public void failureProcessTxnStakeFinishedSession() {
		Session finishedSession = SessionBuilder.aSession().withSessionStatus(SessionStatus.FINISHED).build();
		Mockito.when(sessionService.getSession(Mockito.anyString())).thenReturn(finishedSession);

		TxnRequest request = defaultStakeTxnRequestBuilder()
				.playId("10")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(409)
				.body("code", equalTo("InvalidState"))
				.body("msg", equalTo("Trying to access a finished session"));
	}

	@Test
	public void failureProcessTxnGamecodeMismatch() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.gameCode("invalidGameId")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"))
				.body("msg", equalTo("Gamecode mismatch"));
	}

	@Test
	public void failureProcessTxnSessionAndTxnPlayerIdMismatch() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.playerId("invalidPlayerId")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"))
				.body("msg", equalTo("Session and player mismatch"));
	}

	@Test
	public void givenTxnGameIdSessionGameIdMismatch_whenProcessTxn_thenBadRequest() {
		TxnRequest request = defaultStakeTxnRequestBuilder()
				.txnId("2000-1")
				.build();
		Mockito.doThrow(new BadRequestException("txn gameId does not match session gameId"))
				.when(gameIdService).validateGameId(any(), any());
		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(request)
				.post("/hive/s2s/platform/player/v1/txn")
				.then()
				.log().all()
				.statusCode(400)
				.body("code", equalTo("BadRequest"))
				.body("msg", equalTo("txn gameId does not match session gameId"));
	}

	@Test
	public void givenSetRetryCount_whenRequeueReconTxnRequest_thenReturnStatus()
	{
		var expectedTxnStatus = TxnStatus.RECON;
		var expectedId = "ID";
		int expectedRetryCount = 5;

		Mockito.when(manualReconService.requeueReconTxn(anyString(), anyInt()))
				.thenReturn(expectedTxnStatus);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.queryParam("retryCount", expectedRetryCount)
				.post(String.format("/hive/s2s/platform/player/v1/txn/%s/recon/requeue",expectedId))
				.then()
				.log().all()
				.statusCode(200)
				.body(equalTo(String.format("\"%s\"", expectedTxnStatus.name())));

		Mockito.verify(manualReconService).requeueReconTxn(expectedId, expectedRetryCount);
		Mockito.verifyNoMoreInteractions(manualReconService);
		Mockito.verifyZeroInteractions(txnService, sessionService, gameService, gameIdService);
	}

	@Test
	public void givenNoRetryCount_whenRequeueReconTxnRequest_thenReturnStatus()
	{
		var expectedTxnStatus = TxnStatus.RECON;
		var expectedId = "ID";

		Mockito.when(manualReconService.requeueReconTxn(anyString(), any()))
				.thenReturn(expectedTxnStatus);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.post(String.format("/hive/s2s/platform/player/v1/txn/%s/recon/requeue",expectedId))
				.then()
				.log().all()
				.statusCode(200)
				.body(equalTo(String.format("\"%s\"", expectedTxnStatus.name())));

		Mockito.verify(manualReconService).requeueReconTxn(expectedId, null);
		Mockito.verifyNoMoreInteractions(manualReconService);
		Mockito.verifyZeroInteractions(txnService, sessionService, gameService, gameIdService);
	}
}
