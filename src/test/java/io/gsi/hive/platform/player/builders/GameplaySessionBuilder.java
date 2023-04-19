package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.session.*;

import java.math.BigDecimal;
import java.time.Instant;

public class GameplaySessionBuilder {
    private String id = SessionPresets.SESSIONID;
    private Long creationTime = Instant.now().toEpochMilli();
    private Long lastAccessedTime = Instant.now().toEpochMilli();
    private String playerId = PlayerPresets.PLAYERID;
    private String igpCode = IgpPresets.IGPCODE_IGUANA;
    private String accessToken = AuthorizationPresets.ACCESSTOKEN;
    private Mode mode = Mode.real;
    private String ccyCode = PlayerPresets.CCY_CODE;
    private String lang = PlayerPresets.LANG;
    private String jurisdiction = PlayerPresets.JURISDICTION;
    private BigDecimal balance = WalletPresets.BDBALANCE;
    private boolean authenticated = true;
    private String gameCode = GamePresets.CODE;
    private SessionStatus sessionStatus = SessionStatus.ACTIVE;
    private String sessionToken = "gameplaySession";
    private String authToken = "authToken";
    private String guestToken = "guestToken";
    private String launchReferrer = "goodgames.com";
    private ClientType clientType = ClientType.iOS;


    private GameplaySessionBuilder() {
    }

    public static GameplaySessionBuilder aSession() {
        return new GameplaySessionBuilder();
    }

    public GameplaySessionBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public GameplaySessionBuilder withCreationTime(Long creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public GameplaySessionBuilder withLastAccessedTime(Long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
        return this;
    }

    public GameplaySessionBuilder withPlayerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    public GameplaySessionBuilder withIgpCode(String igpCode) {
        this.igpCode = igpCode;
        return this;
    }

    public GameplaySessionBuilder withAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public GameplaySessionBuilder withMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public GameplaySessionBuilder withCcyCode(String ccyCode) {
        this.ccyCode = ccyCode;
        return this;
    }

    public GameplaySessionBuilder withLang(String lang) {
        this.lang = lang;
        return this;
    }

    public GameplaySessionBuilder withJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
        return this;
    }

    public GameplaySessionBuilder withBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    public GameplaySessionBuilder withAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        return this;
    }

    public GameplaySessionBuilder withSessionStatus(SessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
        return this;
    }

    public GameplaySessionBuilder withSessionToken(SessionStatus sessionStatus) {
        this.sessionToken = sessionToken;
        return this;
    }

    public GameplaySession build() {
        GameplaySession gameplaySession = new GameplaySession();
        gameplaySession.setGameCode(gameCode);
        gameplaySession.setId(id);
        gameplaySession.setCreationTime(creationTime);
        gameplaySession.setLastAccessedTime(lastAccessedTime);
        gameplaySession.setPlayerId(playerId);
        gameplaySession.setIgpCode(igpCode);
        gameplaySession.setAccessToken(accessToken);
        gameplaySession.setMode(mode);
        gameplaySession.setCcyCode(ccyCode);
        gameplaySession.setLang(lang);
        gameplaySession.setJurisdiction(jurisdiction);
        gameplaySession.setBalance(balance);
        gameplaySession.setAuthenticated(authenticated);
        gameplaySession.setSessionStatus(sessionStatus);
        gameplaySession.setSessionToken(sessionToken);
        gameplaySession.setAuthToken(authToken);
        gameplaySession.setGuestToken(guestToken);
        gameplaySession.setLaunchReferrer(launchReferrer);
        gameplaySession.setClientType(clientType);
        return gameplaySession;
    }
}
