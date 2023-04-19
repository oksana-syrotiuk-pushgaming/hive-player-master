package io.gsi.hive.platform.player.session;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.hive.platform.player.exception.SessionNotFoundException;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerKey;
import io.gsi.hive.platform.player.player.PlayerRepository;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

	private final PlayerRepository playerRepository;
	private final SessionRepository sessionRepository;
	private final SessionConfigProperties sessionConfigProperties;
	private final boolean singleUseToken;


	public SessionService(SessionRepository sessionRepository, PlayerRepository playerRepository, SessionConfigProperties sessionConfigProperties,
		@Value("${hive.session.token.single-use:true}") boolean singleUseToken) {
		this.sessionRepository = sessionRepository;
		this.playerRepository = playerRepository;
		this.sessionConfigProperties = sessionConfigProperties;
		this.singleUseToken = singleUseToken;
	}

	/**
	 * @param sessionId
	 * @return the session we found or throws SessionNotFoundException if none found
	 */
	@Timed
	@Transactional //TODO investigate adding (readOnly = true)
	public Session getSession(String sessionId) {
		return sessionRepository.findById(sessionId)
				.orElseThrow(() -> new SessionNotFoundException(
				        "Session not found for Id: " + sessionId));
	}

	/**
	 * @param sessionToken
	 * @return the session we found
	 */
	@Timed
	@Transactional
	public GameplaySession getSessionByToken(String sessionToken) {
		return sessionRepository.findBySessionToken(sessionToken);
	}

	@Timed
	@Transactional
	public void keepalive(Session session) {
		session.setLastAccessedTime(Instant.now().toEpochMilli());
		sessionRepository.save(session);
	}

	/**
	 * save the session to the repository
	 *
	 * @param session
	 */
	@Timed
	public void persistSession(Session session) {
		sessionRepository.save(session);
	}

	private long getAge(Session session) {
		Long now = Instant.now().toEpochMilli();
		Long lastAccessedTime = session.getLastAccessedTime();
		return now - lastAccessedTime;
	}

	public boolean isExpired(Session session) {
		long expiryMillis = TimeUnit.SECONDS.toMillis(sessionConfigProperties.getExpirySecs());
		return Optional.ofNullable(session).isEmpty() || (getAge(session) > expiryMillis)
				|| session.getSessionStatus().equals(SessionStatus.EXPIRED) || session.getSessionStatus().equals(SessionStatus.CLOSED);
	}

	@Transactional
	public Session createSession(SessionCreationLogin loginEvent, PlayerWrapper playerWrapper) {
		playerRepository.findAndLockByPlayerIdAndIgpCodeAndGuest(playerWrapper.getPlayer().getPlayerId(),
				playerWrapper.getPlayer().getIgpCode(), playerWrapper.getPlayer().getGuest());

		finishActiveSession(playerWrapper.getPlayer().getPlayerId(), playerWrapper.getPlayer().getIgpCode(), !playerWrapper.getPlayer().getGuest());

		Session session = new Session();
		Player player = playerWrapper.getPlayer();
		session.setPlayerId(player.getPlayerId());
		//login lang takes preference over player lang if present
		if (loginEvent.getLang()!=null) {
			session.setLang(loginEvent.getLang());
		} else {
			session.setLang(player.getLang());
		}

		if(loginEvent instanceof GuestLogin)
		{
			session.setAccessToken(playerWrapper.getAuthToken());
			session.setAuthenticated(false);
			session.setMode(Mode.demo);
		}
		else{
			if (playerWrapper.getAuthToken() == null || playerWrapper.getAuthToken() == "") {
				playerWrapper.setAuthToken(((PlayerLogin) loginEvent).getAuthToken());
			}
			session.setAccessToken(playerWrapper.getAuthToken());
			session.setAuthenticated(true);
			session.setMode(loginEvent.getMode());
		}
		session.setBalance(playerWrapper.getWallet().getBalance());
		session.setIpAddress(loginEvent.ipAddress);
		session.setUserAgent(loginEvent.userAgent);
		session.setCcyCode(player.getCcyCode());
		session.setIgpCode(player.getIgpCode());
		session.setGameCode(loginEvent.getGameCode());
		session.setJurisdiction(loginEvent.getJurisdiction());

		sessionRepository.save(session);
		return session;
	}

	@Transactional
	public GameplaySession createGameplaySession(GameplaySessionRequest gameplaySessionRequest) {
		playerRepository.findAndLockByPlayerIdAndIgpCodeAndGuest(gameplaySessionRequest.getPlayerId(),
				gameplaySessionRequest.getIgpCode(), gameplaySessionRequest.getGuestToken() != null);

		finishActiveSession(gameplaySessionRequest.getPlayerId(), gameplaySessionRequest.getIgpCode(), gameplaySessionRequest.getGuestToken() == null);

		GameplaySession session = new GameplaySession();
		session.setPlayerId(gameplaySessionRequest.getPlayerId());
		session.setMode(gameplaySessionRequest.mode);
		session.setLang(gameplaySessionRequest.lang);
		session.setAccessToken(gameplaySessionRequest.getAccessToken());
		session.setAuthenticated(gameplaySessionRequest.getGuestToken() == null);
		session.setIpAddress(gameplaySessionRequest.getIpAddress());
		session.setUserAgent(gameplaySessionRequest.getUserAgent());
		session.setCcyCode(gameplaySessionRequest.getCcyCode());
		session.setIgpCode(gameplaySessionRequest.igpCode);
		session.setGameCode(gameplaySessionRequest.gameCode);
		session.setJurisdiction(gameplaySessionRequest.getJurisdiction());
		session.setLaunchReferrer(gameplaySessionRequest.getLaunchReferrer());
		session.setClientType(gameplaySessionRequest.getClientType());
		session.setCountry(gameplaySessionRequest.getCountryCode());
		session.setRegion(gameplaySessionRequest.getRegionCode());
		session.setGuestToken(gameplaySessionRequest.getGuestToken());
		session.setAuthToken(gameplaySessionRequest.getAuthToken());

		sessionRepository.save(session);

		if (playerRepository.findById(new PlayerKey(session.getPlayerId(), session.getIgpCode(), session.getGuestToken() != null)).isEmpty()) {
			playerRepository.saveAndFlush(createPlayer(session));
		}

		return session;
	}

	private Player createPlayer(GameplaySession gameplaySession) {
		Player player = new Player();
		player.setPlayerId(gameplaySession.getPlayerId());
		player.setIgpCode(gameplaySession.getIgpCode());
		player.setGuest(gameplaySession.getGuestToken() != null);
		player.setLang(gameplaySession.getLang());
		player.setCcyCode(gameplaySession.getCcyCode());
		player.setCountry(gameplaySession.getCountry());
		return player;
	}

	private void finishActiveSession(String playerId, String igpCode, boolean authenticated) {
		sessionRepository
				.findByStatusAndPlayerIdAndIgpCodeAndAuthenticated(SessionStatus.ACTIVE, playerId, igpCode, authenticated)
				.forEach(s -> {
					s.setSessionStatus(SessionStatus.FINISHED);
					sessionRepository.save(s);
				});
	}

	public Integer getSessionExpirySecs() {
		return sessionConfigProperties.getExpirySecs();
	}

	public List<Session> getSessions(String playerId, String igpCode) {
		return sessionRepository.findByStatusAndPlayerIdAndIgpCode(SessionStatus.ACTIVE, playerId, igpCode);
	}

	public boolean isSingleUseTokenInvalid(GameplaySession session) {
		return session.getTokenUsed() && singleUseToken;
	}

	@Transactional
	public List<Session> terminatePlayersSessions(List<Session> sessions, String reason) {
		sessions.forEach(session -> {
			session.setSessionStatus(SessionStatus.CLOSED);
			session.setReason(reason);
		});
		sessionRepository.saveAll(sessions);
		return sessions;
	}

	@Transactional
	public Session terminateSession(Session session, String reason) {
		session.setSessionStatus(SessionStatus.CLOSED);
		session.setReason(reason);
		sessionRepository.save(session);
		return session;
	}

}
