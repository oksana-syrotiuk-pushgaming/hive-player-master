package io.gsi.hive.platform.player.api.s2s;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.ForbiddenException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus.Status;
import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.builders.*;
import io.gsi.hive.platform.player.demo.DemoWalletService;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerKey;
import io.gsi.hive.platform.player.player.PlayerService;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.session.*;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import io.gsi.hive.platform.player.wallet.GameplayWallet;
import io.gsi.hive.platform.player.wallet.Wallet;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.gsi.commons.test.string.StringUtils.generateRandomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL}, executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class S2SSessionControllerIT extends ApiITBase {

	private static final String BASE_URI = "/hive/s2s/platform/player/v1";

	@MockBean private MeshService meshServiceMock;
	@MockBean private DemoWalletService demoWalletServiceMock;
	@MockBean private BonusWalletService bonusWalletServiceMock;
	@MockBean private PlayerService playerService;

	@Autowired private SessionService sessionService;

	@Test
	public void okCreatePlayerLoginSession() {

		/*Fund is mutated via addAll in controller to add in bonus funds. the default builder uses Arrays.asList,
		which does not actually produce an ArrayList that supports addAll, resulting in a confusing 501 error*/
		ArrayList<Fund> funds = new ArrayList<>();
		funds.add(new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE));

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withGuest(false).build())
				.withWallet(WalletBuilder.aWallet().withFunds(funds).build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().build();

		when(meshServiceMock.sendAuth(any(),any()))
		.thenReturn(playerWrapper);

		Player player = playerWrapper.getPlayer();

		when(bonusWalletServiceMock.getWallet(
				playerLogin.getIgpCode(),
				player.getPlayerId(),
				playerLogin.getGameCode(),
				player.getCcyCode()))

		.thenReturn(WalletBuilder.aWallet().withFunds(
				Collections.singletonList(FreeroundsFundBuilder.freeroundsFund().build())).build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		Session session = sessionService.getSession(jsonPath.get("sessionId"));
		assertThat(session).isNotNull();
		assertThat(session.getLang()).isEqualTo(playerLogin.getLang());
		assertThat(session.getJurisdiction()).isEqualTo(playerLogin.getJurisdiction());

		assertThat(jsonPath.getObject("wallet", Wallet.class));

		Mockito.verify(playerService).save(player);
		Mockito.verify(meshServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(playerLogin));
		Mockito.verify(bonusWalletServiceMock).getWallet(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, GamePresets.CODE, PlayerPresets.CCY_CODE);
	}

	@Test
	public void shouldCreateSessionDetails_fromValidPlayerInfo() {

		List<Fund> funds = List.of(new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE));
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withGuest(false).build())
				.withWallet(WalletBuilder.aWallet().withFunds(funds).build())
				.build();
		PlayerInfo playerInfo = new PlayerInfo(playerLogin, playerWrapper);
		Player player = playerWrapper.getPlayer();
		Wallet wallet = playerWrapper.getWallet();

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerInfo)
				.post(BASE_URI + "/player/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		Session session = sessionService.getSession(jsonPath.get("sessionId"));
		String responseSessionId = jsonPath.getObject("sessionId", String.class);
		Player responsePlayer = jsonPath.getObject("player", Player.class);
		Wallet responseWallet = jsonPath.getObject("wallet", Wallet.class);

		assertNotNull(session);
		assertEquals(playerLogin.getLang(), session.getLang());
		assertEquals(playerLogin.getJurisdiction(), session.getJurisdiction());
		assertEquals(responsePlayer, player);
		assertEquals(responseWallet, wallet);
		assertEquals(responseSessionId, session.getId());
	}

	@Test
	public void shouldReturn500_whenNonValidPlayerInfoProvided() {

		PlayerInfo playerInfo = new PlayerInfo(new PlayerLogin(), new PlayerWrapper());
		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerInfo)
				.post(BASE_URI + "/player/session")
				.then()
				.log().all()
				.statusCode(500)
				.extract().jsonPath();
	}

	@Test
	public void okCreatePlayerLoginSessionWithIpAddressAndUserAgent() {

		/*Fund is mutated via addAll in controller to add in bonus funds. the default builder uses Arrays.asList,
		which does not actually produce an ArrayList that supports addAll, resulting in a confusing 501 error*/

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withGuest(false).build())
				.withWallet(WalletBuilder.aWallet().build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().build();

		when(meshServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		when(bonusWalletServiceMock.getWallet(any(), any(), any(), any() ))
				.thenReturn(WalletBuilder.aWallet().withFunds(Arrays.asList(FreeroundsFundBuilder.freeroundsFund().build())).build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		Session session = sessionService.getSession(jsonPath.get("sessionId"));
		assertThat(session).isNotNull();
		assertThat(session.getIpAddress()).isNotNull();
		assertThat(session.getUserAgent()).isNotNull();
		assertThat(session.getLang()).isEqualTo(playerLogin.getLang());
		assertThat(session.getJurisdiction()).isEqualTo(playerLogin.getJurisdiction());

		Mockito.verify(playerService).save(playerWrapper.getPlayer());
		Mockito.verify(meshServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(playerLogin));
		Mockito.verify(bonusWalletServiceMock).getWallet(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, GamePresets.CODE, PlayerPresets.CCY_CODE);
	}

	@Test
	public void okCreateGuestDemoLoginSession() {

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withGuest(true).build())
				.withWallet(WalletBuilder.aWallet().build())
				.build();

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().withMode(Mode.demo).build();

		when(demoWalletServiceMock.sendAuth(any(),any()))
		.thenReturn(playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(guestLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		Session session = sessionService.getSession(jsonPath.get("sessionId"));
		assertThat(session).isNotNull();
		assertThat(session.getLang()).isEqualTo(guestLogin.getLang());
		assertThat(session.getJurisdiction()).isEqualTo(guestLogin.getJurisdiction());


		Mockito.verify(playerService).save(playerWrapper.getPlayer());
		Mockito.verify(demoWalletServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(guestLogin));
	}

	@Test
	public void givenGuestLoginWithAuth_whenCreateSession_returnValidSession() {

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withGuest(true).build())
				.withWallet(WalletBuilder.aWallet().build())
				.build();

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin()
				.withMode(Mode.demo)
				.withAuthToken(AuthorizationPresets.ACCESSTOKEN)
				.build();

		when(demoWalletServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(guestLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		Session session = sessionService.getSession(jsonPath.get("sessionId"));
		assertThat(session).isNotNull();
		assertThat(session.getLang()).isEqualTo(guestLogin.getLang());
		assertThat(session.getJurisdiction()).isEqualTo(guestLogin.getJurisdiction());
		assertThat(session.getAccessToken()).isEqualTo(guestLogin.getAuthToken());

		Mockito.verify(playerService).save(playerWrapper.getPlayer());
		Mockito.verify(demoWalletServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(guestLogin));
	}

	@Test
	public void givenGuestLoginWithNonGuestIgp_whenCreateSession_returnForbidden() {

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().withMode(Mode.demo).build();

		when(demoWalletServiceMock.sendAuth(any(),any()))
		.thenThrow(new ForbiddenException("IGP is not enabled for Guest Play"));

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(guestLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(403);

		Mockito.verify(demoWalletServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(guestLogin));
	}

	@Test
	public void failureGuestLoginApiUnauthorised() {

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin()
				.withMode(Mode.demo)
				.withAuthToken(AuthorizationPresets.ACCESSTOKEN)
				.build();

		when(demoWalletServiceMock.sendAuth(any(),any()))
				.thenThrow(new AuthorizationException(""));

		RestAssured.given()
					.log().all()
					.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
					.accept(ContentType.JSON)
					.contentType(ContentType.JSON)
					.body(guestLogin)
					.post(BASE_URI+"/session")
				.then()
					.log().all()
					.statusCode(401);

		Mockito.verify(demoWalletServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(guestLogin));
	}

	@Test
	public void givenSessionTokenLogin_whenLogin_thenGameplaySessionReturned() {
		SessionTokenLogin sessionTokenLogin = new SessionTokenLogin();
		sessionTokenLogin.setSessionToken("gameplaySession");

		GameplaySession gameplaySession = GameplaySessionBuilder.aSession().build();
		assertFalse(gameplaySession.getTokenUsed());

		sessionService.persistSession(gameplaySession);

		when(meshServiceMock.getGameplayWallet(
				gameplaySession.getIgpCode(),
				gameplaySession.getPlayerId(),
				gameplaySession.getGameCode(),
				gameplaySession.getAccessToken()))
				.thenReturn(GameplayWalletBuilder.aWallet().build());

		when(bonusWalletServiceMock.getWallet(any(),any(),any(), any())).thenReturn(WalletBuilder.freeroundsWallet().withFunds(new ArrayList<>()).build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(sessionTokenLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		GameplaySession session = sessionService.getSessionByToken(jsonPath.get("session.sessionToken"));
		assertThat(session).isNotNull();
		assertTrue(session.getTokenUsed());

		GameplayWallet gameplayWallet = jsonPath.getObject("wallet", GameplayWallet.class);

		assertNotNull(gameplayWallet);
		assertEquals(WalletPresets.BDBALANCE, gameplayWallet.getBalance());

		Mockito.verify(meshServiceMock).getGameplayWallet(gameplaySession.getIgpCode(), gameplaySession.getPlayerId(), gameplaySession.getGameCode(), gameplaySession.getAccessToken());
	}

	@Test
	public void givenSessionTokenLoginWithBonusFund_whenLogin_thenGameplaySessionReturned() {
		SessionTokenLogin sessionTokenLogin = new SessionTokenLogin();
		sessionTokenLogin.setSessionToken("gameplaySession");

		GameplaySession gameplaySession = GameplaySessionBuilder.aSession().build();
		assertFalse(gameplaySession.getTokenUsed());

		sessionService.persistSession(gameplaySession);

		when(meshServiceMock.getGameplayWallet(
				gameplaySession.getIgpCode(),
				gameplaySession.getPlayerId(),
				gameplaySession.getGameCode(),
				gameplaySession.getAccessToken()))
				.thenReturn(GameplayWalletBuilder.aWallet().build());

		when(bonusWalletServiceMock.getWallet(any(),any(),any(), any())).thenReturn(WalletBuilder.freeroundsWallet().build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(sessionTokenLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		GameplaySession session = sessionService.getSessionByToken(jsonPath.get("session.sessionToken"));
		assertThat(session).isNotNull();
		assertTrue(session.getTokenUsed());

		GameplayWallet gameplayWallet = jsonPath.getObject("wallet", GameplayWallet.class);

		assertNotNull(gameplayWallet);
		assertEquals(WalletPresets.BDBALANCE, gameplayWallet.getBalance());
		assertEquals(1, gameplayWallet.getFunds().size());
		FreeroundsFund fund = (FreeroundsFund) gameplayWallet.getFunds().get(0);
		assertEquals(FundType.FREEROUNDS, fund.getType());
		assertEquals("bonus1", fund.getBonusId());

		Mockito.verify(meshServiceMock).getGameplayWallet(gameplaySession.getIgpCode(), gameplaySession.getPlayerId(), gameplaySession.getGameCode(), gameplaySession.getAccessToken());
	}

	@Test
	public void givenDemoSessionTokenLogin_whenLogin_thenGameplaySessionReturned() {
		SessionTokenLogin sessionTokenLogin = new SessionTokenLogin();
		sessionTokenLogin.setSessionToken("gameplaySession");

		GameplaySession gameplaySession = GameplaySessionBuilder.aSession().withMode(Mode.demo).build();
		assertFalse(gameplaySession.getTokenUsed());

		sessionService.persistSession(gameplaySession);

		when(demoWalletServiceMock.getGameplayWallet(
				gameplaySession.getIgpCode(),
				gameplaySession.getPlayerId(),
				gameplaySession.getGameCode()))
				.thenReturn(GameplayWalletBuilder.aWallet().build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(sessionTokenLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		GameplaySession session = sessionService.getSessionByToken(jsonPath.get("session.sessionToken"));
		assertThat(session).isNotNull();
		assertTrue(session.getTokenUsed());

		GameplayWallet gameplayWallet = jsonPath.getObject("wallet", GameplayWallet.class);

		assertNotNull(gameplayWallet);
		assertEquals(WalletPresets.BDBALANCE, gameplayWallet.getBalance());

		Mockito.verify(demoWalletServiceMock).getGameplayWallet(gameplaySession.getIgpCode(), gameplaySession.getPlayerId(), gameplaySession.getGameCode());
	}

	@Test
	public void givenUsedSessionTokenLogin_whenLogin_thenBadRequestThrown() {
		SessionTokenLogin sessionTokenLogin = new SessionTokenLogin();
		sessionTokenLogin.setSessionToken("gameplaySession");

		GameplaySession gameplaySession = GameplaySessionBuilder.aSession().withMode(Mode.demo).build();
		gameplaySession.setTokenUsed(true);

		sessionService.persistSession(gameplaySession);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(sessionTokenLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(400)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("BadRequest");
	}

	@Test
	public void givenIncorrectSessionToken_whenLogin_thenSessionNotFound() {
		SessionTokenLogin sessionTokenLogin = new SessionTokenLogin();
		sessionTokenLogin.setSessionToken("incorrectSession");

		GameplaySession gameplaySession = GameplaySessionBuilder.aSession().build();

		sessionService.persistSession(gameplaySession);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(sessionTokenLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(404)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("SessionNotFound");
	}

	@Test
	public void okGetPlayer() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		when(meshServiceMock.getPlayer(any(),any()))
		.thenReturn(player);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/player")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPath.getString("playerId")).isEqualTo(playerWrapper.getPlayer().getPlayerId());
		//we expect the player lang not the login lang
		assertThat(jsonPath.getString("lang")).isEqualTo("fr");

		Mockito.verify(meshServiceMock).getPlayer(eq(IgpPresets.IGPCODE_IGUANA), eq(player.getPlayerId()));
	}

	@Test
	public void okGetWallet() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();


		Session session = sessionService.createSession(playerLogin, playerWrapper);

		when(meshServiceMock.getWallet(any(),any(), any(), any())).thenReturn(wallet);
		when(bonusWalletServiceMock.getWallet(any(),any(),any(), any())).thenReturn(WalletBuilder.freeroundsWallet().withFunds(new ArrayList<>()).build());

		JsonPath jsonPath = RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/wallet")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPath.getString("balance")).isEqualTo(wallet.getBalance().toString());

		Mockito.verify(meshServiceMock).getWallet(eq(IgpPresets.IGPCODE_IGUANA), eq(player.getPlayerId()), eq(playerLogin.getGameCode()), eq(playerLogin.getAuthToken()));
	}

	@Test
	public void okGetWalletWithFreeroundsFund() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();


		Session session = sessionService.createSession(playerLogin, playerWrapper);

		Wallet bonusWallet = WalletBuilder.freeroundsWallet().build();
		when(meshServiceMock.getWallet(any(),any(), any(), any())).thenReturn(wallet);
		when(bonusWalletServiceMock.getWallet(any(),any(),any(), any())).thenReturn(bonusWallet);

		Wallet actualWallet = RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/wallet")
				.then()
				.log().all()
				.statusCode(200)
				.extract()
				.jsonPath()
				.getObject("", Wallet.class);
		assertThat(actualWallet.getBalance()).isEqualTo(wallet.getBalance());
		assertThat(actualWallet.getFunds().size()).isEqualTo(2);
		assertThat(actualWallet.getFunds().get(0)).isEqualToComparingFieldByFieldRecursively(wallet.getFunds().get(0));
		assertThat(actualWallet.getFunds().get(1)).isEqualToIgnoringGivenFields(bonusWallet.getFunds().get(0), "betAmount");
		Mockito.verify(meshServiceMock).getWallet(eq(IgpPresets.IGPCODE_IGUANA), eq(player.getPlayerId()), eq(playerLogin.getGameCode()), eq(playerLogin.getAuthToken()));
	}

	@Test
	public void failGetWalletSessionNotFound() {
		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/session123/wallet")
				.then()
				.log().all()
				.statusCode(404)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("SessionNotFound");
	}

	@Test
	public void failGetPlayerSessionNotFound() {
		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/session123/player")
				.then()
				.log().all()
				.statusCode(404)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("SessionNotFound");
	}

	@Test
	public void playerLoginCurrencyMismatch() {

		/*Fund is mutated via addAll in controller to add in bonus funds. the default builder uses Arrays.asList,
		which does not actually produce an ArrayList that supports addAll, resulting in a confusing 501 error*/
		ArrayList<Fund> funds = new ArrayList<>();
		funds.add(new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE));

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withCcyCode("EUR").withGuest(false).build())
				.withWallet(WalletBuilder.aWallet().withFunds(funds).build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().build();

		when(meshServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(400)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("BadRequest");
	}

	@Test
	public void playerLoginPlayerIdMismatch() {

		/*Fund is mutated via addAll in controller to add in bonus funds. the default builder uses Arrays.asList,
		which does not actually produce an ArrayList that supports addAll, resulting in a confusing 501 error*/
		ArrayList<Fund> funds = new ArrayList<>();
		funds.add(new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE));

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withPlayerId("differentPlayerId").withGuest(false).build())
				.withWallet(WalletBuilder.aWallet().withFunds(funds).build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().build();

		when(meshServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(401)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("Authorization");
	}


	@Test
	public void okGetSessionDetails() {
		Player player = PlayerBuilder.aPlayer().build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();


		Session session = sessionService.createSession(playerLogin, playerWrapper);

		when(playerService.get(any())).thenReturn(player);
		when(meshServiceMock.getWallet(any(),any(), any(), any())).thenReturn(wallet);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId())
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPath.getString("sessionId")).isEqualTo(session.getId());
		assertThat(jsonPath.getString("lang")).isEqualTo("en");

		assertThat(jsonPath.getObject("wallet", Wallet.class)).isNotNull();
		assertThat(jsonPath.getObject("wallet", Wallet.class).getBalance()).isEqualTo(wallet.getBalance());
		assertThat(jsonPath.getObject("player", Player.class)).isNotNull();
		assertThat(jsonPath.getObject("player", Player.class).getPlayerId()).isEqualTo(player.getPlayerId());

		Mockito.verify(meshServiceMock, times(0)).getPlayer(any(), any());
		Mockito.verify(playerService).get(new PlayerKey(PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA, false));

		Mockito.verify(meshServiceMock).getWallet(IgpPresets.IGPCODE_IGUANA, player.getPlayerId(), session.getGameCode(), session.getAccessToken());
	}

	@Test
	public void failureGetSessionDetailsNotFound() {

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+"INVALID")
				.then()
				.log().all()
				.statusCode(404)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("SessionNotFound");
	}

	@Test
	public void failHivePlayerApiKeyMissing() {

		RestAssured.given()
				.log().all()
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body("{}")
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(401);
	}

	@Test
	public void failHivePlayerApiKeyInvalid() {

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, "invalid-key")
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body("{}")
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(401);
	}

	@Test
	public void failGetSessionDetailsExpiredSession() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);
		session.setLastAccessedTime(1L);
		sessionService.persistSession(session);

		when(meshServiceMock.getWallet(any(),any(), any(), any())).thenReturn(wallet);

		RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId())
				.then()
				.log().all()
				.statusCode(404)
				.body("code", equalTo("SessionNotFound"))
				.body("msg", equalTo("Session not found for Id: "+session.getId()));

		Mockito.verify(meshServiceMock, times(0)).getWallet(any(), any(), any(), any());
	}

	@Test
	public void failGetSessionDetailsFinishedSession() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);
		session.setSessionStatus(SessionStatus.FINISHED);
		sessionService.persistSession(session);

		when(meshServiceMock.getWallet(any(),any(), any(), any())).thenReturn(wallet);

		RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId())
				.then()
				.log().all()
				.statusCode(409)
				.body("code", equalTo("InvalidState"))
				.body("msg", equalTo("Trying to access a finished session"));

		Mockito.verify(meshServiceMock, times(0)).getWallet(any(), any(), any(), any());
	}

	@Test
	public void failGetPlayerExpiredSession() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);
		session.setLastAccessedTime(1L);
		sessionService.persistSession(session);

		when(meshServiceMock.getPlayer(any(),any())).thenReturn(player);

		RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/player")
				.then()
				.log().all()
				.statusCode(404)
				.body("code", equalTo("SessionNotFound"))
				.body("msg", equalTo("no session could be found"));

		Mockito.verify(meshServiceMock, times(0)).getPlayer(any(), any());
	}

	@Test
	public void failGetPlayerFinishedSession() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);
		session.setSessionStatus(SessionStatus.FINISHED);
		sessionService.persistSession(session);

		when(meshServiceMock.getPlayer(any(),any())).thenReturn(player);

		RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/player")
				.then()
				.log().all()
				.statusCode(409)
				.body("code", equalTo("InvalidState"))
				.body("msg", equalTo("Trying to access a finished session"));

		Mockito.verify(meshServiceMock, times(0)).getPlayer(any(), any());
	}

	@Test
	public void failGetWalletExpiredSession() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);
		session.setLastAccessedTime(1L);
		sessionService.persistSession(session);

		when(meshServiceMock.getWallet(any(),any(), any(), any())).thenReturn(wallet);

		RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/wallet")
				.then()
				.log().all()
				.statusCode(404)
				.body("code", equalTo("SessionNotFound"))
				.body("msg", equalTo("no session could be found"));

		Mockito.verify(meshServiceMock, times(0)).getWallet(any(), any(), any(), any());
	}

	@Test
	public void failGetWalletFinishedSession() {
		Player player = PlayerBuilder.aPlayer().withLang("fr").build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);
		session.setSessionStatus(SessionStatus.FINISHED);
		sessionService.persistSession(session);

		when(meshServiceMock.getWallet(any(),any(), any(), any())).thenReturn(wallet);

		RestAssured.given()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/wallet")
				.then()
				.log().all()
				.statusCode(409)
				.body("code", equalTo("InvalidState"))
				.body("msg", equalTo("Trying to access a finished session"));

		Mockito.verify(meshServiceMock, times(0)).getWallet(any(), any(), any(), any());
	}

	@Test
	public void givenNoTokenReturnedFromIgp_whenCreateSession_savesSessionWithTokenFromLoginEvent() {
		ArrayList<Fund> funds = new ArrayList<>();
		funds.add(new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE));

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withGuest(false).build())
				.withWallet(WalletBuilder.aWallet().withFunds(funds).build())
				.withAuthToken(null)
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().build();

		when(meshServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		when(bonusWalletServiceMock.getWallet(any(), any(), any(),any()))
				.thenReturn(WalletBuilder.aWallet().withFunds(Arrays.asList(FreeroundsFundBuilder.freeroundsFund().build())).build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		Session session = sessionService.getSession(jsonPath.get("sessionId"));
		assertThat(session).isNotNull();
		assertThat(session.getLang()).isEqualTo(playerLogin.getLang());
		assertThat(session.getJurisdiction()).isEqualTo(playerLogin.getJurisdiction());

		assertThat(jsonPath.getObject("wallet", Wallet.class)).isNotNull();

		assertThat(session.getAccessToken()).isEqualTo(playerLogin.getAuthToken());

		Mockito.verify(playerService).save(playerWrapper.getPlayer());
		Mockito.verify(meshServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(playerLogin));
		Mockito.verify(bonusWalletServiceMock).getWallet(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, GamePresets.CODE, PlayerPresets.CCY_CODE);
	}

	@Test
	public void givenTokenReturnedFromIgp_whenCreateSession_savesSessionWithTokenFromIgp() {
		ArrayList<Fund> funds = new ArrayList<>();
		funds.add(new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE));

		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().withGuest(false).build())
				.withWallet(WalletBuilder.aWallet().withFunds(funds).build())
				.withAuthToken(generateRandomString(256))
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().build();

		when(meshServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		when(bonusWalletServiceMock.getWallet(any(), any(), any(), any() ))
				.thenReturn(WalletBuilder.aWallet().withFunds(Arrays.asList(FreeroundsFundBuilder.freeroundsFund().build())).build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		Session session = sessionService.getSession(jsonPath.get("sessionId"));
		assertThat(session).isNotNull();
		assertThat(session.getLang()).isEqualTo(playerLogin.getLang());
		assertThat(session.getJurisdiction()).isEqualTo(playerLogin.getJurisdiction());

		assertThat(jsonPath.getObject("wallet", Wallet.class)).isNotNull();

		assertThat(session.getAccessToken()).isEqualTo(playerWrapper.getAuthToken());

		Mockito.verify(playerService).save(playerWrapper.getPlayer());
		Mockito.verify(meshServiceMock).sendAuth(eq(IgpPresets.IGPCODE_IGUANA), eq(playerLogin));
		Mockito.verify(bonusWalletServiceMock).getWallet(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, GamePresets.CODE, PlayerPresets.CCY_CODE);
	}


	@Test
	public void failCreatePlayerLoginSessionInvalidLanguage() {
		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin()
				.withLang("EN")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI + "/session")
				.then()
				.log().all()
				.statusCode(400);
	}

	@Test
	public void failCreatePlayerLoginSessionInvalidLangLength() {
		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin()
				.withLang("e")
				.build();

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI + "/session")
				.then()
				.log().all()
				.statusCode(400);
	}

	@Test
	public void getBonusAwardStatusOk() {

		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(WalletBuilder.aWallet().build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();
		Session session = sessionService.createSession(playerLogin, playerWrapper);

		when(bonusWalletServiceMock.getBonusAwardStatus(any(), any()))
				.thenReturn(FreeRoundsBonusPlayerAwardStatus.builder().status(Status.expired).build());

		var status = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/fund/" + WalletPresets.BONUSFUNDID+ "/status")
				.then()
				.log().all()
				.statusCode(200)
				.extract().as(FreeRoundsBonusPlayerAwardStatus.class);

		assertThat(status.getStatus()).isEqualTo(Status.expired);

	}

	@Test
	public void failGetOfflineWalletNoSession() {
		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/notARealSession/offlineBonusWallet")
				.then()
				.log().all()
				.statusCode(404);
	}

	@Test
	public void failGetBonusAwardStatusWithDemoMode() {

		Player player = PlayerBuilder.aPlayer().withGuest(true).build();
		Wallet wallet = WalletBuilder.aWallet().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(wallet)
				.build();

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().withMode(Mode.demo).build();
		Session session = sessionService.createSession(guestLogin, playerWrapper);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/" + session.getId()+"/fund/" + WalletPresets.BONUSFUNDID + "/status")
				.then()
				.log().all()
				.statusCode(409);


	}

	@Test
	public void failGetBonusAwardStatusWithNoSession() {

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/" + SessionPresets.SESSIONID +"/fund/" + WalletPresets.BONUSFUNDID + "/status")
				.then()
				.log().all()
				.statusCode(404);

	}

	@Test
	public void failGetBonusAwardStatusFinishedSession() {

		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.withWallet(WalletBuilder.aWallet().build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();
		Session session = sessionService.createSession(playerLogin, playerWrapper);
		//create new session which finishes the previous one
		sessionService.createSession(playerLogin, playerWrapper);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session.getId()+"/fund/" + WalletPresets.BONUSFUNDID+ "/status")
				.then()
				.log().all()
				.statusCode(409);

	}

	@Test
	public void givenSessionRequestWithoutJurisdiction_whenCreateSession_thenResponsePlayerHasNoJurisdiction(){
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().build())
				.withWallet(WalletBuilder.aWallet().build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().withJurisdiction("").build();

		when(meshServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		when(bonusWalletServiceMock.getWallet(any(), any(), any(),any()))
				.thenReturn(WalletBuilder.aWallet().build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		String jurisdiction = jsonPath.get("player.jurisdiction");
		assertThat(jurisdiction).isEmpty();
	}

	@Test
	public void givenSessionRequestWithJurisidction_whenCreateSession_thenResponsePlayerHasExpectedJurisdiction(){
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(PlayerBuilder.aPlayer().build())
				.withWallet(WalletBuilder.aWallet().build())
				.build();

		PlayerLogin playerLogin = PlayerLoginBuilder
				.aPlayerLogin().withJurisdiction("UK").build();

		when(meshServiceMock.sendAuth(any(),any()))
				.thenReturn(playerWrapper);

		when(bonusWalletServiceMock.getWallet(any(), any(), any(),any()))
				.thenReturn(WalletBuilder.aWallet().build());

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(playerLogin)
				.post(BASE_URI+"/session")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		String jurisdiction = jsonPath.get("player.jurisdiction");
		assertThat(jurisdiction).isEqualTo("UK");
	}

	@Test
	public void givenActiveSessionId_whenTerminateSession_thenActiveSessionTerminated() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/session/"+session.getId()+"/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPath.getString("id")).isEqualTo(session.getId());
		assertThat(jsonPath.getString("sessionStatus")).isEqualTo(SessionStatus.CLOSED.name());
		assertThat(jsonPath.getString("reason")).isEqualTo("Terminate message");
	}

	@Test
	public void givenIncorrectIgpCodeForSession_whenTerminateSession_thenActiveSessionTerminated() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/incorrect/session/"+session.getId()+"/terminate")
				.then()
				.log().all()
				.statusCode(409)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("InvalidState");
		assertThat(jsonPath.getString("msg")).isEqualTo("Igp Code does not match session igp code");
	}

	@Test
	public void givenActiveSessionIdAndNoReason_whenTerminateSession_thenActiveSessionTerminated() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/session/"+session.getId()+"/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPath.getString("id")).isEqualTo(session.getId());
		assertThat(jsonPath.getString("sessionStatus")).isEqualTo(SessionStatus.CLOSED.name());
		assertThat(jsonPath.getString("reason")).isNull();
	}

	@Test
	public void givenTerminatedSessionId_whenTerminateSession_thenInvalidStateExceptionThrown() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/session/"+session.getId()+"/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/session/"+session.getId()+"/terminate")
				.then()
				.log().all()
				.statusCode(409)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("InvalidState");
		assertThat(jsonPath.getString("msg")).isEqualTo("Trying to access an expired session");
	}

	@Test
	public void givenInvalidSessionId_whenTerminateSession_thenSessionNotFoundExceptionThrown() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);
		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/session/invalid/terminate")
				.then()
				.log().all()
				.statusCode(404)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("SessionNotFound");
		assertThat(jsonPath.getString("msg")).isEqualTo("Session not found for Id: invalid");
	}

	@Test
	public void givenOneActiveSession_whenTerminatePlayerSessions_thenActiveSessionTerminated() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/"+player.getPlayerId()+"/session/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPath.getString("[0].id")).isEqualTo(session.getId());
		assertThat(jsonPath.getString("[0].sessionStatus")).isEqualTo(SessionStatus.CLOSED.name());
		assertThat(jsonPath.getString("[0].reason")).isEqualTo("Terminate message");
	}

	@Test
	public void givenOnlyTerminatedSession_whenTerminatePlayerSessions_thenInvalidStateExceptionThrown() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		sessionService.createSession(playerLogin, playerWrapper);

		RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/"+player.getPlayerId()+"/session/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/"+player.getPlayerId()+"/session/terminate")
				.then()
				.log().all()
				.statusCode(409)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("InvalidState");
		assertThat(jsonPath.getString("msg")).isEqualTo("No active sessions for playerId: player1");
	}

	@Test
	public void givenInvalidPlayerId_whenTerminatePlayerSessions_thenSessionNotFoundExceptionThrown() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		sessionService.createSession(playerLogin, playerWrapper);
		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/invalidPlayerId/session/terminate")
				.then()
				.log().all()
				.statusCode(409)
				.extract().jsonPath();

		assertThat(jsonPath.getString("code")).isEqualTo("InvalidState");
		assertThat(jsonPath.getString("msg")).isEqualTo("No active sessions for playerId: invalidPlayerId");
	}

	@Test
	public void givenTwoActiveSessionsWithDifferingIgpCodes_whenTerminatePlayerSessions_thenOneActiveSessionTerminated() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		Wallet wallet = Wallet.builder().build();

		when(playerService.get(any())).thenReturn(player);
		when(meshServiceMock.getWallet(any(),any(),any(),any())).thenReturn(wallet);

		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		PlayerLogin playerLogin2 = PlayerLoginBuilder.aPlayerLogin().withGameCode("game_code_2")
				.withIgpCode("igp_code_2").build();
		Player player2 = PlayerBuilder.aPlayer().withIgpCode("igp_code_2").build();
		PlayerWrapper playerWrapper2 = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player2)
				.build();

		Session session2 = sessionService.createSession(playerLogin2, playerWrapper2);

		JsonPath jsonPathEndedSession = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/"+player.getPlayerId()+"/session/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		JsonPath jsonPathActiveSession = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.get(BASE_URI+"/session/"+session2.getId())
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPathEndedSession.getString("[0].id")).isEqualTo(session.getId());
		assertThat(jsonPathEndedSession.getString("[0].sessionStatus")).isEqualTo(SessionStatus.CLOSED.name());
		assertThat(jsonPathActiveSession.getString("sessionId")).isEqualTo(session2.getId());
	}

	@Test
	public void givenTwoDifferentSessions_whenTerminatePlayerSessions_thenEachSessionTerminated() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		JsonPath firstSession = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/"+player.getPlayerId()+"/session/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(firstSession.getString("[0].id")).isEqualTo(session.getId());
		assertThat(firstSession.getString("[0].sessionStatus")).isEqualTo(SessionStatus.CLOSED.name());

		Session session2 = sessionService.createSession(playerLogin, playerWrapper);

		JsonPath secondSession = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.queryParam("reason","Terminate message")
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/"+player.getPlayerId()+"/session/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(secondSession.getString("[0].id")).isEqualTo(session2.getId());
		assertThat(secondSession.getString("[0].sessionStatus")).isEqualTo(SessionStatus.CLOSED.name());
	}

	@Test
	public void givenActiveSessionIdAndNoReason_whenTerminatePlayerSessions_thenActiveSessionTerminated() {
		Player player = PlayerBuilder.aPlayer().build();
		PlayerWrapper playerWrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(player)
				.build();
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();

		Session session = sessionService.createSession(playerLogin, playerWrapper);

		JsonPath jsonPath = RestAssured.given()
				.log().all()
				.header(HivePlayerAuthInterceptor.API_KEY_NAME, hivePlayerApiKeyValue)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.post(BASE_URI+"/igp/"+player.getIgpCode()+"/player/"+player.getPlayerId()+"/session/terminate")
				.then()
				.log().all()
				.statusCode(200)
				.extract().jsonPath();

		assertThat(jsonPath.getString("[0].id")).isEqualTo(session.getId());
		assertThat(jsonPath.getString("[0].sessionStatus")).isEqualTo(SessionStatus.CLOSED.name());
		assertThat(jsonPath.getString("[0].reason")).isNull();
	}
}