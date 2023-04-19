package io.gsi.hive.platform.player.api.s2s;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.exception.SessionNotFoundException;
import io.gsi.hive.platform.player.session.GameplaySession;
import io.gsi.hive.platform.player.session.GameplaySessionDetails;
import io.gsi.hive.platform.player.session.GameplaySessionRequest;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.session.SessionTokenDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/s2s/platform/player/v2")
@Loggable
public class S2SGameplaySessionController {

    private final SessionService sessionService;
    private static final String INVALID_STATE_EXCEPTION = "Trying to access a finished session";

    public S2SGameplaySessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping(path="/session")
    public SessionTokenDetails createSession(@RequestBody @Valid GameplaySessionRequest sessionRequest) {
        GameplaySession gameplaySession = sessionService.createGameplaySession(sessionRequest);
        return new SessionTokenDetails(gameplaySession.getSessionToken());
    }

    /**
     * @return session details
     */
    @GetMapping(path = "/session/{sessionToken}")
    public GameplaySessionDetails getGameplaySessionDetails(@PathVariable("sessionToken") String sessionToken) {
        GameplaySession session = sessionService.getSessionByToken(sessionToken);

        if (sessionService.isExpired(session)) {
            throw new SessionNotFoundException("Session not found for Token: " + sessionToken);
        }
        if (session.isFinished()) {
            throw new InvalidStateException(INVALID_STATE_EXCEPTION);
        }

        return new GameplaySessionDetails(session);
    }
}