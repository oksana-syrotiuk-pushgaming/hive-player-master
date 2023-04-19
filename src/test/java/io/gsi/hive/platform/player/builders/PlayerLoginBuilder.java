package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.AuthorizationPresets;
import io.gsi.hive.platform.player.presets.ClientPresets;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.session.ClientType;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.PlayerLogin;

public final class PlayerLoginBuilder {
	private Long timestamp;

	private String igpCode;
	private String siteId;
	private String gameCode;
	private String lang;
	private String jurisdiction;
	private Mode mode;
	private String currency;

	private String ipAddress;
	private String userAgent;
	private ClientType clientType;

	private String authToken;
	private String playerId;
	private Integer rcMins;

	private PlayerLoginBuilder() {
		this.timestamp = 1L;

		this.igpCode = IgpPresets.IGPCODE_IGUANA;
		this.siteId = null;
		this.gameCode = GamePresets.CODE;
		this.lang = PlayerPresets.LANG;
		this.jurisdiction = PlayerPresets.JURISDICTION;
		this.mode = Mode.real;
		this.currency = WalletPresets.CURRENCY;
		this.ipAddress = ClientPresets.IPADDRESS;
		this.userAgent = ClientPresets.USERAGENT;
		this.clientType = ClientType.HTML;

		this.authToken = AuthorizationPresets.ACCESSTOKEN;
		this.playerId = PlayerPresets.PLAYERID;
		this.rcMins = null;
	}

	public static PlayerLoginBuilder aPlayerLogin() {
		return new PlayerLoginBuilder();
	}

	public PlayerLoginBuilder withAuthToken(String authToken) {
		this.authToken = authToken;
		return this;
	}

	public PlayerLoginBuilder withPlayerId(String playerId) {
		this.playerId = playerId;
		return this;
	}

	public PlayerLoginBuilder withRcMins(Integer rcMins) {
		this.rcMins = rcMins;
		return this;
	}
	
	public PlayerLoginBuilder withTimestamp(Long timeStamp) {
		this.timestamp = timeStamp;
		return this;
	}

	public PlayerLoginBuilder withIgpCode(String igpCode) {
		this.igpCode = igpCode;
		return this;
	}

	public PlayerLoginBuilder withSiteId(String siteId) {
		this.siteId = siteId;
		return this;
	}

	public PlayerLoginBuilder withGameCode(String gameCode) {
		this.gameCode = gameCode;
		return this;
	}

	public PlayerLoginBuilder withMode(Mode mode) {
		this.mode = mode;
		return this;
	}

	public PlayerLoginBuilder withCurrency(String currency) {
		this.currency = currency;
		return this;
	}

	public PlayerLoginBuilder withLang(String lang) {
		this.lang = lang;
		return this;
	}

	public PlayerLoginBuilder withJurisdiction(String jurisdiction){
		this.jurisdiction = jurisdiction;
		return this;
	}
	
    public PlayerLoginBuilder withIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
		return this;
	}

	public PlayerLoginBuilder withUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}

	public PlayerLoginBuilder withClientType(ClientType clientType) {
		this.clientType = clientType;
		return this;
	}

	public PlayerLogin build() {		
		PlayerLogin playerLogin = new PlayerLogin();
		playerLogin.setAuthToken(authToken);
		playerLogin.setPlayerId(playerId);
		playerLogin.setRcMins(rcMins);
		playerLogin.setTimestamp(timestamp);
		playerLogin.setTimestamp(1L);
		playerLogin.setIgpCode(igpCode);
		playerLogin.setSiteId(siteId);
		playerLogin.setGameCode(gameCode);
		playerLogin.setMode(mode);
		playerLogin.setCurrency(currency);
		playerLogin.setLang(lang);
		playerLogin.setJurisdiction(jurisdiction);
		playerLogin.setClientType(clientType);
		playerLogin.setIpAddress(ipAddress);
		playerLogin.setUserAgent(userAgent);
		return playerLogin;
	}
}
