package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.session.ClientType;
import io.gsi.hive.platform.player.session.GameplaySessionRequest;
import io.gsi.hive.platform.player.session.Mode;

public final class GameplaySessionRequestBuilder {
    private final String playerId = PlayerPresets.PLAYERID;
    private final String authToken = null;
    private final String guestToken = "guestToken";
    private final String accessToken = AuthorizationPresets.ACCESSTOKEN;
    private final String igpCode = IgpPresets.IGPCODE_IGUANA;
    private final String integrationCode = "push";
    private final String gameCode = GamePresets.CODE;
    private final Mode mode = Mode.demo;
    private final String ccyCode = PlayerPresets.CCY_CODE;
    private final String countryCode = PlayerPresets.COUNTRY;
    private final String regionCode = "CON";
    private final String jurisdiction = PlayerPresets.JURISDICTION;
    private final String lang = PlayerPresets.LANG;
    private final ClientType clientType = ClientType.iOS;
    private final String launchReferrer = "goodgames.com";
    private final String ipAddress = ClientPresets.IPADDRESS;
    private final String userAgent = ClientPresets.USERAGENT;

    private GameplaySessionRequestBuilder() {
    }

    public static GameplaySessionRequestBuilder aSession() {
        return new GameplaySessionRequestBuilder();
    }

    public GameplaySessionRequest build() {
        GameplaySessionRequest gameplaySessionRequest = new GameplaySessionRequest();
        gameplaySessionRequest.setPlayerId(playerId);
        gameplaySessionRequest.setAuthToken(authToken);
        gameplaySessionRequest.setGuestToken(guestToken);
        gameplaySessionRequest.setAccessToken(accessToken);
        gameplaySessionRequest.setIgpCode(igpCode);
        gameplaySessionRequest.setIntegrationCode(integrationCode);
        gameplaySessionRequest.setGameCode(gameCode);
        gameplaySessionRequest.setMode(mode);
        gameplaySessionRequest.setCcyCode(ccyCode);
        gameplaySessionRequest.setCountryCode(countryCode);
        gameplaySessionRequest.setRegionCode(regionCode);
        gameplaySessionRequest.setJurisdiction(jurisdiction);
        gameplaySessionRequest.setLang(lang);
        gameplaySessionRequest.setClientType(clientType);
        gameplaySessionRequest.setLaunchReferrer(launchReferrer);
        gameplaySessionRequest.setIpAddress(ipAddress);
        gameplaySessionRequest.setUserAgent(userAgent);
        return gameplaySessionRequest;
    }
}
