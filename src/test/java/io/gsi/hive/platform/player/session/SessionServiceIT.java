package io.gsi.hive.platform.player.session;

import static io.gsi.commons.test.string.StringUtils.generateRandomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.Assert.*;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.GameplaySessionBuilder;
import io.gsi.hive.platform.player.builders.GameplaySessionRequestBuilder;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.exception.SessionNotFoundException;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.wallet.Wallet;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL}, executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SessionServiceIT extends ApiITBase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private SessionService sessionService;

	private String randomString;

	@Before
	public void Setup() {
		randomString = String.join("", Collections.nCopies(257, "A"));
	}

	@Autowired
	private SessionRepository sessionRepository;

	@Test
	public void getSession() {
		Session session = new Session();
		session.setCcyCode(PlayerPresets.CCY_CODE);
		sessionService.persistSession(session);
		Session result = sessionService.getSession(session.getId());
		assertThat(result).isEqualTo(session);
	}

	@Test
	public void getSessionNonExistentId() {
		thrown.expect(SessionNotFoundException.class);
		sessionService.getSession("non-existent-id");
	}

	@Test
	public void keepalive() {
		Session session = new Session();
		session.setLastAccessedTime(10000L);
		session.setCcyCode(PlayerPresets.CCY_CODE);
		sessionService.persistSession(session);
		sessionService.keepalive(session);
		Session keptAliveSession = sessionService.getSession(session.getId());
		assertThat(keptAliveSession.getLastAccessedTime()).isGreaterThan(10000L);
	}

	@Test
	public void givenNoActiveSessions_whenCreateGameplaySession_thenReturnGameplaySession(){
		GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
		GameplaySession gameplaySession = sessionService.createGameplaySession(gameplaySessionRequest);

		List<Session> activeSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(activeSessions).hasSize(1);
		GameplaySession activeGameplaySession = (GameplaySession) activeSessions.get(0);
		assertEquals(PlayerPresets.PLAYERID, activeGameplaySession.getPlayerId());
		assertNotNull(activeGameplaySession.getSessionToken());
		assertFalse(activeGameplaySession.isAuthenticated());

		List<Player> players = playerRepository.findAll();
		assertThat(players).hasSize(1);
		Player player = players.get(0);
		assertEquals(gameplaySessionRequest.getPlayerId(), player.getPlayerId());

		assertEquals(PlayerPresets.PLAYERID, gameplaySession.getPlayerId());
		assertEquals(SessionStatus.ACTIVE, gameplaySession.getSessionStatus());
		assertEquals("goodgames.com", gameplaySession.getLaunchReferrer());
	}

	@Test
	public void givenNonGuestSession_whenCreateGameplaySession_thenReturnGameplaySession(){
		GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
		gameplaySessionRequest.setGuestToken(null);
		gameplaySessionRequest.setAuthToken("authToken");

		GameplaySession gameplaySession = sessionService.createGameplaySession(gameplaySessionRequest);

		List<Session> activeSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(activeSessions).hasSize(1);
		GameplaySession activeGameplaySession = (GameplaySession) activeSessions.get(0);
		assertEquals(PlayerPresets.PLAYERID, activeGameplaySession.getPlayerId());
		assertNotNull(activeGameplaySession.getSessionToken());
		assertTrue(activeGameplaySession.isAuthenticated());

		List<Player> players = playerRepository.findAll();
		assertThat(players).hasSize(1);
		Player player = players.get(0);
		assertEquals(gameplaySessionRequest.getPlayerId(), player.getPlayerId());
		assertEquals(false, player.getGuest());

		assertEquals(PlayerPresets.PLAYERID, gameplaySession.getPlayerId());
		assertEquals(SessionStatus.ACTIVE, gameplaySession.getSessionStatus());
		assertEquals("goodgames.com", gameplaySession.getLaunchReferrer());
	}

	@Test
	public void givenActiveSession_whenCreateGameplaySession_thenPersistNewSession(){
		GameplaySession oldGameplaySession = GameplaySessionBuilder.aSession().build();
		oldGameplaySession.setAuthenticated(false);
		sessionService.persistSession(oldGameplaySession);

		GameplaySessionRequest gameplaySessionRequest = GameplaySessionRequestBuilder.aSession().build();
		GameplaySession gameplaySession = sessionService.createGameplaySession(gameplaySessionRequest);

		List<Session> allSessions = sessionRepository.findAll();

		List<Session> activeSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertEquals(2, allSessions.size());
		assertEquals(SessionStatus.FINISHED, sessionRepository.findById(SessionPresets.SESSIONID).get().getSessionStatus());

		GameplaySession activeGameplaySession = (GameplaySession) activeSessions.get(0);
		assertThat(activeSessions).hasSize(1);
		assertEquals(PlayerPresets.PLAYERID, activeGameplaySession.getPlayerId());
		assertNotNull(activeGameplaySession.getSessionToken());

		assertEquals(PlayerPresets.PLAYERID, gameplaySession.getPlayerId());
		assertEquals(SessionStatus.ACTIVE, gameplaySession.getSessionStatus());
		assertEquals("goodgames.com", gameplaySession.getLaunchReferrer());
	}

	@Test
	public void givenExistingSessionToken_whenGetGameplaySession_thenReturnCorrectGameplaySession() {
		GameplaySession session = new GameplaySession();
		session.setCcyCode(PlayerPresets.CCY_CODE);
		sessionService.persistSession(session);
		Session result = sessionService.getSessionByToken(session.getSessionToken());
		assertEquals(session, result);
	}

	@Test
	public void givenNonExistentSessionToken_whenGetGetGameplaySession_thenReturnNullSession() {
		assertNull(sessionService.getSessionByToken("nonExistentSessionToken"));
	}

	@Test
	public void givenCreateSession_whenExistingActiveSession_thenPersistNewSession() {
		Session session = new Session();
		session.setPlayerId(PlayerPresets.PLAYERID);
		session.setCcyCode(PlayerPresets.CCY_CODE);
		session.setSessionStatus(SessionStatus.ACTIVE);
		session.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		session.setAuthenticated(true);

		sessionService.persistSession(session);

		PlayerLogin login = getPlayerLogin();

		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = PlayerBuilder.aPlayer().build();
		player.setPlayerId(PlayerPresets.PLAYERID);
		player.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		player.setCcyCode(PlayerPresets.CCY_CODE);
		playerWrapper.setPlayer(player);
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("1.00"));
		playerWrapper.setWallet(wallet);

		Session newSession = sessionService.createSession(login, playerWrapper);
		Session newSessionFromRepository = sessionService.getSession(newSession.getId());

		assertThat(newSessionFromRepository.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);

		List<Session> activeSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(activeSessions).hasSize(1);

		List<Session> finishedSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.FINISHED, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(finishedSessions).hasSize(1);
	}

	/**
	 * Used to test the rare edge case where the player ends up with two or more active sessions
 	 */
	@Test
	public void givenCreateSession_whenMultipleExistingActiveSession_thenPersistNewSession() {
		Session session1 = new Session();
		session1.setPlayerId(PlayerPresets.PLAYERID);
		session1.setSessionStatus(SessionStatus.ACTIVE);
		session1.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		session1.setCcyCode(PlayerPresets.CCY_CODE);
		session1.setAuthenticated(true);

		sessionService.persistSession(session1);

		Session session2 = new Session();
		session2.setPlayerId(PlayerPresets.PLAYERID);
		session2.setSessionStatus(SessionStatus.ACTIVE);
		session2.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		session2.setCcyCode(PlayerPresets.CCY_CODE);
		session2.setAuthenticated(true);

		sessionService.persistSession(session2);

		List<Session> existingActiveSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(existingActiveSessions).hasSize(2);

		PlayerLogin login = getPlayerLogin();

		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = PlayerBuilder.aPlayer().build();
		player.setPlayerId(PlayerPresets.PLAYERID);
		player.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		player.setCcyCode(PlayerPresets.CCY_CODE);
		playerWrapper.setPlayer(player);
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("1.00"));
		playerWrapper.setWallet(wallet);

		Session newSession = sessionService.createSession(login, playerWrapper);
		Session newSessionFromRepository = sessionService.getSession(newSession.getId());

		assertThat(newSessionFromRepository.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);

		List<Session> activeSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(activeSessions).hasSize(1);

		List<Session> finishedSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.FINISHED, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(finishedSessions).hasSize(2);
	}



	@Test
	public void givenCreateSession_whenExistingActiveSession_thenPersistNewSessionTruncationOfUserAgent() {
		Session session = new Session();
		session.setPlayerId(PlayerPresets.PLAYERID);
		session.setCcyCode(PlayerPresets.CCY_CODE);
		session.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		session.setSessionStatus(SessionStatus.ACTIVE);
		session.setAuthenticated(true);

		sessionService.persistSession(session);

		PlayerLogin login = getPlayerLoginUserAgentAbove256Length();

		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = PlayerBuilder.aPlayer().build();
		player.setPlayerId(PlayerPresets.PLAYERID);
		player.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		player.setCcyCode(PlayerPresets.CCY_CODE);
		playerWrapper.setPlayer(player);
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("1.00"));
		playerWrapper.setWallet(wallet);

		Session newSession = sessionService.createSession(login, playerWrapper);
		Session newSessionFromRepository = sessionService.getSession(newSession.getId());

		assertThat(newSessionFromRepository.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);

		List<Session> activeSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(activeSessions).hasSize(1);

		List<Session> finishedSessions = sessionRepository
				.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(finishedSessions).hasSize(1);

		assertThat(activeSessions.get(0).getUserAgent().length()).isEqualTo(256);
	}

	@Test
	public void givenPlayerInDb_whenConcurrentThreadsCreateSessions_thenOnlyOneActiveSessionCreated() throws InterruptedException {

		Player testPlayer = PlayerBuilder.aPlayer().build();
		playerRepository.saveAndFlush(testPlayer);

		PlayerLogin login = getPlayerLogin();
		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = PlayerBuilder.aPlayer().build();
		player.setPlayerId(PlayerPresets.PLAYERID);
		player.setIgpCode(IgpPresets.IGPCODE_IGUANA);
		player.setCcyCode(PlayerPresets.CCY_CODE);
		playerWrapper.setPlayer(player);
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("1.00"));
		playerWrapper.setWallet(wallet);

		ExecutorService executorService = Executors.newFixedThreadPool(4);
		CountDownLatch countDownLatch = new CountDownLatch(4);

		for(int i = 0; i < 4; i++) {
			executorService.execute(() -> {
				sessionService.createSession(login, playerWrapper);
				countDownLatch.countDown();
			});
		}

		countDownLatch.await();

		assertThat(sessionRepository.findAll().size()).isEqualTo(4);

		assertThat(sessionRepository.findByStatusAndPlayerIdAndIgpCode(SessionStatus.FINISHED, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA).size())
				.isEqualTo(3);

		assertThat(sessionRepository.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA).size())
				.isEqualTo(1);

	}

	@Test
	public void givenNoTokenReturnedFromIgp_whenCreateSession_returnsPlayerWrapperWithTokenFromLoginEvent() {
		PlayerLogin login = getPlayerLoginWithToken();

		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = PlayerBuilder.aPlayer().build();
		playerWrapper.setPlayer(player);
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("1.00"));
		playerWrapper.setWallet(wallet);

		Session newSession = sessionService.createSession(login, playerWrapper);
		Session newSessionFromRepository = sessionService.getSession(newSession.getId());

		assertThat(newSessionFromRepository.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);

		assertThat(newSessionFromRepository.getAccessToken()).isEqualTo(login.getAuthToken());
	}

	@Test
	public void givenBlankStringTokenReturnedFromIgp_whenCreateSession_returnsPlayerWrapperWithTokenFromLoginEvent() {
		PlayerLogin login = getPlayerLoginWithToken();

		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = PlayerBuilder.aPlayer().build();
		playerWrapper.setPlayer(player);
		playerWrapper.setAuthToken("");
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("1.00"));
		playerWrapper.setWallet(wallet);

		Session newSession = sessionService.createSession(login, playerWrapper);
		Session newSessionFromRepository = sessionService.getSession(newSession.getId());

		assertThat(newSessionFromRepository.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);

		assertThat(newSessionFromRepository.getAccessToken()).isEqualTo(login.getAuthToken());
	}

	@Test
	public void givenTokenReturnedFromIgp_whenCreateSession_returnsPlayerWrapperWithTokenFromIgp() {
		PlayerLogin login = getPlayerLoginWithToken();

		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = PlayerBuilder.aPlayer().build();
		player.setPlayerId(PlayerPresets.PLAYERID);
		player.setCcyCode(PlayerPresets.CCY_CODE);
		playerWrapper.setPlayer(player);
		playerWrapper.setAuthToken(generateRandomString(256));
		Wallet wallet = new Wallet();
		wallet.setBalance(new BigDecimal("1.00"));
		playerWrapper.setWallet(wallet);

		Session newSession = sessionService.createSession(login, playerWrapper);
		Session newSessionFromRepository = sessionService.getSession(newSession.getId());

		assertThat(newSessionFromRepository.getSessionStatus()).isEqualTo(SessionStatus.ACTIVE);

		assertThat(newSessionFromRepository.getAccessToken()).isEqualTo(playerWrapper.getAuthToken());
	}

	@Test
	public void checkSessionStatusExpired() {
		Session session = new Session();
		session.setSessionStatus(SessionStatus.EXPIRED);

		assertTrue(sessionService.isExpired(session));
	}

	@Test
	public void givenExpiredSession_whenIsExpired_returnsTrue() {
		Session session = new Session();
		session.setPlayerId(PlayerPresets.PLAYERID);
		session.setSessionStatus(SessionStatus.ACTIVE);
		session.setLastAccessedTime(1L);
		assertTrue(sessionService.isExpired(session));
	}

	@Test
	public void givenNonExpiredSession_whenIsExpired_returnsFalse() {
		Session session = new Session();
		session.setPlayerId(PlayerPresets.PLAYERID);
		session.setSessionStatus(SessionStatus.ACTIVE);
		assertFalse(sessionService.isExpired(session));
	}

	@Test
	public void givenTerminatedSession_whenIsExpired_returnsTrue() {
		Session session = new Session();
		session.setPlayerId(PlayerPresets.PLAYERID);
		session.setSessionStatus(SessionStatus.CLOSED);
		assertTrue(sessionService.isExpired(session));
	}

	@Test
	public void givenPlayerHasTwoSessions_whenGetSessions_returnsTwoSessions() {
		Session session = new Session();
		session.setPlayerId("playerId");
		session.setIgpCode("iguana");
		session.setCcyCode(PlayerPresets.CCY_CODE);
		Session session2 = new Session();
		session2.setPlayerId("playerId");
		session2.setIgpCode("iguana");
		session2.setCcyCode(PlayerPresets.CCY_CODE);

		sessionService.persistSession(session);
		sessionService.persistSession(session2);

		List<Session> sessions = sessionService.getSessions("playerId", "iguana");

		assertEquals(2, sessions.size());
	}

	@Test
	public void givenPlayerHasNoSessions_whenGetSessions_returnsZeroSessions() {
		List<Session> sessions = sessionService.getSessions("playerId", "iguana");

		assertEquals(0, sessions.size());
	}

	@Test
	public void givenActiveSession_whenTerminateSession_sessionIsTerminated() {
		Session session = new Session();
		session.setSessionStatus(SessionStatus.ACTIVE);
		session.setPlayerId("playerId");
		session.setIgpCode("iguana");
		session.setCcyCode(PlayerPresets.CCY_CODE);

		sessionService.persistSession(session);

		Session terminatedSession = sessionService.terminateSession(session, "Terminate message");

		assertEquals(SessionStatus.CLOSED, terminatedSession.getSessionStatus());
		assertEquals("Terminate message", terminatedSession.getReason());
	}

	@Test
	public void givenTerminatedSession_whenTerminateSession_sessionIsTerminated() {
		Session session = new Session();
		session.setSessionStatus(SessionStatus.CLOSED);
		session.setPlayerId("playerId");
		session.setIgpCode("iguana");
		session.setCcyCode(PlayerPresets.CCY_CODE);

		sessionService.persistSession(session);

		Session terminatedSession = sessionService.terminateSession(session, "Terminate message");

		assertEquals(SessionStatus.CLOSED, terminatedSession.getSessionStatus());
		assertEquals("Terminate message", terminatedSession.getReason());
	}

	@Test
	public void givenTwoActiveSessions_terminatePlayersSessions_returnsTwoSessions() {
		Session session = new Session();
		session.setPlayerId("playerId");
		session.setIgpCode("iguana");
		session.setSessionStatus(SessionStatus.ACTIVE);
		session.setCcyCode(PlayerPresets.CCY_CODE);
		Session session2 = new Session();
		session2.setPlayerId("playerId");
		session2.setIgpCode("iguana");
		session2.setSessionStatus(SessionStatus.ACTIVE);
		session2.setCcyCode(PlayerPresets.CCY_CODE);

		sessionService.persistSession(session);
		sessionService.persistSession(session2);

		List<Session> terminatedSessions = sessionService.terminatePlayersSessions(newArrayList(session, session2), "Terminate message");

		assertEquals(2, terminatedSessions.size());
		assertEquals(SessionStatus.CLOSED, terminatedSessions.get(0).getSessionStatus());
		assertEquals("Terminate message", terminatedSessions.get(0).getReason());
		assertEquals(SessionStatus.CLOSED, terminatedSessions.get(1).getSessionStatus());
		assertEquals("Terminate message", terminatedSessions.get(1).getReason());
	}

	@Test
	public void givenActiveSessionWithReasonOverLimit_whenTerminateSession_sessionIsTerminatedAndReasonCondensed() {
		Session session = new Session();
		session.setSessionStatus(SessionStatus.ACTIVE);
		session.setPlayerId("playerId");
		session.setIgpCode("iguana");
		session.setCcyCode(PlayerPresets.CCY_CODE);

		sessionService.persistSession(session);

		Session terminatedSession = sessionService.terminateSession(session, randomString);

		assertEquals(SessionStatus.CLOSED, terminatedSession.getSessionStatus());
		assertEquals(256, terminatedSession.getReason().length());
	}

	@Test
	public void givenSingleUseTokenNotUsed_whenSingleUseTokenInvalidCheck_thenReturnFalse() {
		GameplaySession session = GameplaySessionBuilder.aSession().build();
		assertFalse(sessionService.isSingleUseTokenInvalid(session));
	}

	@Test
	public void givenSingleUseTokenUsed_whenSingleUseTokenInvalidCheck_thenReturnTrue() {
		GameplaySession session = GameplaySessionBuilder.aSession().build();
		session.setTokenUsed(true);
		assertTrue(sessionService.isSingleUseTokenInvalid(session));
	}

	private PlayerLogin getPlayerLogin() {
		PlayerLogin login = new PlayerLogin();
		login.setPlayerId(PlayerPresets.PLAYERID);
		login.setIpAddress("192.168.0.1");
		login.setUserAgent("thisIsATest");
		return login;
	}

	private PlayerLogin getPlayerLoginWithToken() {
		PlayerLogin login = new PlayerLogin();
		login.setPlayerId(PlayerPresets.PLAYERID);
		login.setIpAddress("192.168.0.1");
		login.setUserAgent("thisIsATest");
		login.setAuthToken(generateRandomString(256));
		return login;
	}

	private PlayerLogin getPlayerLoginUserAgentAbove256Length() {
		PlayerLogin login = new PlayerLogin();
		login.setPlayerId(PlayerPresets.PLAYERID);
		login.setIpAddress("192.168.0.1");
		login.setUserAgent(randomString);
		return login;
	}
}
