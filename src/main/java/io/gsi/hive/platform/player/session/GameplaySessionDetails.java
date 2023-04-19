package io.gsi.hive.platform.player.session;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameplaySessionDetails {
    private String sessionId;
    private String sessionToken;
    private String igpCode;
    private String authToken;
    private String guestToken;
    private String accessToken;
    private String playerId;
    private String gameCode;
    private Mode mode;
    private ClientType clientType;
    private String launchReferrer;

    public GameplaySessionDetails(GameplaySession session) {
        this.sessionId = session.getId();
        this.sessionToken = session.getSessionToken();
        this.igpCode = session.getIgpCode();
        this.authToken = session.getAuthToken();
        this.guestToken = session.getGuestToken();
        this.accessToken = session.getAccessToken();
        this.playerId = session.getPlayerId();
        this.gameCode = session.getGameCode();
        this.mode = session.getMode();
        this.clientType = session.getClientType();
        this.launchReferrer = session.getLaunchReferrer();
    }
}
