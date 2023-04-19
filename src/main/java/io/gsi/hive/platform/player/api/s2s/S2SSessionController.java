/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.api.s2s;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus;
import io.gsi.hive.platform.player.demo.DemoWalletService;
import io.gsi.hive.platform.player.event.EventType;
import io.gsi.hive.platform.player.exception.SessionNotFoundException;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerKey;
import io.gsi.hive.platform.player.player.PlayerService;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.session.*;
import io.gsi.hive.platform.player.wallet.GameplayWallet;
import io.gsi.hive.platform.player.wallet.Wallet;
import io.gsi.hive.platform.player.wallet.WalletDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static java.util.Optional.ofNullable;

@RestController
@RequestMapping("/s2s/platform/player/v1")
@Loggable
public class S2SSessionController {

	private final PlayerService playerService;
	private final SessionService sessionService;
	private final MeshService meshService;
	private final DemoWalletService demoWalletService;
	private final BonusWalletService bonusWalletService;
	//This removes the need to have bonuswallet spun up for e2e testing
	private final boolean retrieveBonusOnCreateSession;

	private static final String INVALID_STATE_EXCEPTION = "Trying to access a finished session";
	private static final String UNKNOWN_GAME_MODE = "Unknown game mode: ";

	public S2SSessionController(@Value("${hive.session.retrieveBonusOnCreate:true}")
										boolean retrieveBonusOnCreateSession,
								PlayerService playerService, SessionService sessionService, MeshService meshService,
								DemoWalletService demoWalletService, BonusWalletService bonusWalletService) {
		this.retrieveBonusOnCreateSession = retrieveBonusOnCreateSession;
		this.playerService = playerService;
		this.sessionService = sessionService;
		this.meshService = meshService;
		this.demoWalletService = demoWalletService;
		this.bonusWalletService = bonusWalletService;
	}

	/**
	 * @param loginEvent
	 * @return session details
	 */
	@PostMapping(path="/session")
	public SessionDetails login(@RequestBody @Valid Login loginEvent) {
		SessionDetails sessionDetails;
		if (loginEvent.getType().equals(EventType.sessionTokenLogin)) {
			SessionTokenLogin sessionTokenLogin = (SessionTokenLogin) loginEvent;
			sessionDetails = sessionTokenLogin(sessionTokenLogin);
		}
		else {
			SessionCreationLogin creationLogin = (SessionCreationLogin) loginEvent;
			sessionDetails = sessionCreationLogin(creationLogin);
		}
		return sessionDetails;
	}

	private SessionDetails sessionCreationLogin(SessionCreationLogin loginEvent) {
		PlayerWrapper playerWrapper = null;

		switch(loginEvent.getMode()) {
			case real:
				playerWrapper = realPlayLogin(loginEvent);
				break;
			case demo:
				playerWrapper = demoWalletService.sendAuth(loginEvent.getIgpCode(), loginEvent);
				break;
			default:
				throw new InvalidStateException(
						"Unknown login mode: " + loginEvent.getMode().toString());
		}

		Player player = playerWrapper.getPlayer();
		//create player if not already there
		playerService.save(player);

		Session session = sessionService.createSession(loginEvent, playerWrapper);

		return createSessionDetails(player, playerWrapper.getWallet(), session);
	}

	private SessionDetails sessionTokenLogin(SessionTokenLogin sessionTokenLogin) {
		GameplaySession session = sessionService.getSessionByToken(sessionTokenLogin.getSessionToken());
		if (session == null) {
			throw new SessionNotFoundException("no session could be found");
		}
		if (sessionService.isSingleUseTokenInvalid(session)) {
			throw new BadRequestException("Token has been used.");
		}
		GameplayWallet wallet;
		switch(session.getMode()) {
			case real:
				wallet = meshService.getGameplayWallet(session.getIgpCode(), session.getPlayerId(), session.getGameCode(), session.getAccessToken());
				if (retrieveBonusOnCreateSession) {
					Wallet bonusWallet = bonusWalletService.getWallet(
							session.getIgpCode(),
							session.getPlayerId(),
							session.getGameCode(),
							session.getCcyCode());
					wallet.setFunds(bonusWallet.getFunds());
				}
				break;
			case demo:
				wallet = demoWalletService.getGameplayWallet(session.getIgpCode(), session.getPlayerId(), session.getGameCode());
				break;
			default:
				throw new InvalidStateException(
						"Unknown login mode: " + session.getMode().toString());
		}
		session.setTokenUsed(true);
		sessionService.persistSession(session);

		return createGameplaySessionDetails(session, wallet);
	}

    private LoginSessionDetails createGameplaySessionDetails(GameplaySession session, GameplayWallet wallet) {
		LoginSessionDetails loginSessionDetails = new LoginSessionDetails();
		loginSessionDetails.setWallet(new WalletDetails(wallet));
		loginSessionDetails.setSession(new GameplaySessionDetails(session));
        return loginSessionDetails;
    }

	protected SessionDetails createSessionDetails(Player player, Wallet wallet, Session session) {
		player.setJurisdiction(session.getJurisdiction());

		SessionCreationDetails sessionDetails = new SessionCreationDetails();
		sessionDetails.setPlayer(player);
		sessionDetails.setWallet(wallet);
		sessionDetails.setSessionId(session.getId());
		sessionDetails.setLang(session.getLang());
		sessionDetails.setMode(session.getMode());
		sessionDetails.setGameCode(session.getGameCode());
		return sessionDetails;
	}

	private PlayerWrapper realPlayLogin(SessionCreationLogin loginEvent) {
		PlayerWrapper playerWrapper;
		if (loginEvent instanceof GuestLogin) {
			throw new InvalidStateException("no real play for guests");
		}
		playerWrapper = meshService.sendAuth(loginEvent.getIgpCode(), loginEvent);
		final var player = playerWrapper.getPlayer();

		validateCurrenciesMatch(loginEvent, player);
		validatePlayerIdMatch((PlayerLogin) loginEvent, player);

		if (retrieveBonusOnCreateSession) {
			Wallet bonusWallet = bonusWalletService.getWallet(
					loginEvent.getIgpCode(),
					player.getPlayerId(),
					loginEvent.getGameCode(),
					player.getCcyCode());
			//We only expect one bonus fund, for now
			playerWrapper.getWallet().getFunds().addAll(bonusWallet.getFunds());
		}
		return playerWrapper;
	}

	private void validateCurrenciesMatch(SessionCreationLogin loginEvent, Player player) {
		var loginHasCcy = loginEvent.getCurrency() != null;
		var loginAndPlayerCcyMismatch = loginHasCcy && !player.getCcyCode()
				.equals(loginEvent.getCurrency());

		if (loginAndPlayerCcyMismatch) {
			throw new BadRequestException("login currency and player currency do not match");
		}
	}

	private void validatePlayerIdMatch(PlayerLogin loginEvent, Player player) {
		final var playerId = ofNullable(loginEvent.getPlayerId());

		boolean loginAndAuthPlayerIdMismatch = playerId.isPresent() && !playerId.get().equals(player.getPlayerId());

		if (loginAndAuthPlayerIdMismatch) {
			throw new AuthorizationException("login and auth playerIds do not match");
		}
	}

	@PostMapping(path="/player/session")
	public SessionDetails createPlayerSession(@RequestBody PlayerInfo playerInfo) {

		PlayerWrapper playerWrapper = playerInfo.getPlayerWrapper();
		Player player = playerWrapper.getPlayer();
		playerService.save(player);
		Session session = sessionService.createSession((SessionCreationLogin) playerInfo.getLogin(), playerWrapper);
		return createSessionDetails(player, playerWrapper.getWallet(), session);
	}

	/**
	 * @return session details
	 *///TODO: should this retrieve freeround bonus funds?
	@GetMapping(path="/session/{sessionId}")
	public SessionDetails getSessionDetails(@PathVariable("sessionId") String sessionId) {

		Session session = sessionService.getSession(sessionId);
		if (sessionService.isExpired(session)) {
			throw new SessionNotFoundException("Session not found for Id: " + sessionId);
		}
		if (session.isFinished()) {
			throw new InvalidStateException(INVALID_STATE_EXCEPTION);
		}

		//Rather than calling getPLayer on the service beyond, we just build it from the info we already have
		Player player = playerService.get(new PlayerKey(session.getPlayerId(), session.getIgpCode(), !session.isAuthenticated()));

		Wallet wallet = null;
		switch(session.getMode())
		{
		case real:{
			//TODO do we need to also get bonus wallet here?
			wallet = meshService.getWallet(session.getIgpCode(), session.getPlayerId(), session.getGameCode(), session.getAccessToken());
			break;}
		case demo:{
			wallet = demoWalletService.getWallet(session.getIgpCode(), session.getPlayerId(), session.getGameCode());
			break;}
		default:{throw new InvalidStateException(UNKNOWN_GAME_MODE +session.getMode().toString());}
		}

        return createSessionDetails(player, wallet, session);
	}

	/**
	 * @param sessionId
	 * @return player
	 */
	@GetMapping(path="/session/{sessionId}/player")
	public Player getPlayer(@PathVariable("sessionId") String sessionId) {

		Session session = sessionService.getSession(sessionId);
		if (sessionService.isExpired(session)) {
			throw new SessionNotFoundException("no session could be found");
		}
		if (session.isFinished()) {
			throw new InvalidStateException(INVALID_STATE_EXCEPTION);
		}

		Player player = null;
		switch(session.getMode())
		{
		case real:{
			player = meshService.getPlayer(session.getIgpCode(), session.getPlayerId());
			break;}
		case demo:{
			player = demoWalletService.getPlayer(session.getIgpCode(), session.getPlayerId());
			break;}
		default:{throw new InvalidStateException(UNKNOWN_GAME_MODE +session.getMode().toString());}
		}

		return player;
	}

	/**
	 * @param sessionId
	 * @return wallet
	 */
	@GetMapping(path = "/session/{sessionId}/wallet")
	public Wallet getWallet(@PathVariable("sessionId") String sessionId) {
		Session session = sessionService.getSession(sessionId);
		if (sessionService.isExpired(session)) {
			throw new SessionNotFoundException("no session could be found");
		}
		if (session.isFinished()) {
			throw new InvalidStateException(INVALID_STATE_EXCEPTION);
		}

		switch (session.getMode()) {
			case real: {
				Wallet wallet = meshService.getWallet(session.getIgpCode(), session.getPlayerId(), session.getGameCode(), session.getAccessToken());
				Wallet bonusWallet = bonusWalletService.getWallet(session.getIgpCode(), session.getPlayerId(), session.getGameCode(), session.getCcyCode());
				wallet.getFunds().addAll(bonusWallet.getFunds());
				return wallet;
			}
			case demo: {
				return demoWalletService.getWallet(session.getIgpCode(), session.getPlayerId(), session.getGameCode());
			}
			default: {
				throw new InvalidStateException(UNKNOWN_GAME_MODE + session.getMode().toString());
			}
		}
	}

	@GetMapping(path = "/session/{sessionId}/fund/{fundId}/status")
	public FreeRoundsBonusPlayerAwardStatus getBonusAwardStatus(@PathVariable("sessionId") String sessionId,
			@PathVariable("fundId") Long fundId) {

		Session session = sessionService.getSession(sessionId);
		if(session == null) {
			throw new SessionNotFoundException("No session could be found.");
		}

		if (session.isFinished()) {
			throw new InvalidStateException(INVALID_STATE_EXCEPTION);
		}
		if(session.getMode() != Mode.real) {
			throw new InvalidStateException("Can only return real bonus award statuses.");
		}

		return  bonusWalletService.getBonusAwardStatus(session.getIgpCode(), fundId);
	}

	@PostMapping(path = "/igp/{igpCode}/session/{sessionId}/terminate")
	public Session terminateSession(@PathVariable("sessionId") String sessionId, @PathVariable("igpCode") String igpCode,
									@RequestParam(required = false) String reason) {
		Session session = sessionService.getSession(sessionId);

		if (sessionService.isExpired(session)) {
			throw new InvalidStateException("Trying to access an expired session");
		}
		if (!session.getIgpCode().equals(igpCode)){
			throw new InvalidStateException("Igp Code does not match session igp code");
		}

		return sessionService.terminateSession(session, reason);
	}

	@PostMapping(path = "/igp/{igpCode}/player/{playerId}/session/terminate")
	public List<Session> terminatePlayerSessions(@PathVariable("playerId") String playerId, @PathVariable("igpCode") String igpCode,
			@RequestParam(required = false) String reason) {
		List<Session> sessions = sessionService.getSessions(playerId, igpCode);

		if (sessions.isEmpty()) {
			throw new InvalidStateException("No active sessions for playerId: " + playerId);
		}

		return sessionService.terminatePlayersSessions(sessions, reason);
	}
}
