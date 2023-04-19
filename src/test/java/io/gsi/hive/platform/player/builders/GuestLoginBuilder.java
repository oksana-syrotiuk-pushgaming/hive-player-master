package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.ClientPresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.session.ClientType;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.session.Mode;

public final class GuestLoginBuilder {
	private Long timestamp;

	private String igpCode;
	private String siteId;
	private String gameCode;
	private String lang;
	private Mode mode;
	private String currency;
	private String ipAddress;
	private String userAgent;
	private ClientType clientType;
	private String authToken;
	private String jurisdiction;

	private GuestLoginBuilder() {
		this.timestamp = 1L;

		this.igpCode = IgpPresets.IGPCODE_IGUANA;
		this.siteId = null;
		this.gameCode = "1000";
		this.lang = PlayerPresets.LANG;
		this.jurisdiction = PlayerPresets.JURISDICTION;
		this.mode = Mode.real;
		this.currency = WalletPresets.CURRENCY;
		this.ipAddress = ClientPresets.IPADDRESS;
		this.userAgent = ClientPresets.USERAGENT;
		this.clientType = ClientType.HTML;
	}

	public static GuestLoginBuilder aGuestLogin(){
		return new GuestLoginBuilder();
	}

	public GuestLoginBuilder withTimestamp(Long timeStamp) {
		this.timestamp = timeStamp;
		return this;
	}

	public GuestLoginBuilder withIgpCode(String igpCode) {
		this.igpCode = igpCode;
		return this;
	}

	public GuestLoginBuilder withSiteId(String siteId) {
		this.siteId = siteId;
		return this;
	}

	public GuestLoginBuilder withGameCode(String gameCode) {
		this.gameCode = gameCode;
		return this;
	}

	public GuestLoginBuilder withMode(Mode mode) {
		this.mode = mode;
		return this;
	}

	public GuestLoginBuilder withCurrency(String currency) {
		this.currency = currency;
		return this;
	}

	public GuestLoginBuilder withLang(String lang) {
		this.lang = lang;
		return this;
	}

	public GuestLoginBuilder withIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
		return this;
	}

	public GuestLoginBuilder withUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}

	public GuestLoginBuilder withClientType(ClientType clientType) {
		this.clientType = clientType;
		return this;
	}

	public GuestLoginBuilder withAuthToken(String authToken) {
		this.authToken = authToken;
		return this;
	}

	public GuestLogin build() {
		GuestLogin guestLogin = new GuestLogin();
		guestLogin.setTimestamp(timestamp);
		guestLogin.setIgpCode(igpCode);
		guestLogin.setSiteId(siteId);
		guestLogin.setGameCode(gameCode);
		guestLogin.setMode(mode);
		guestLogin.setCurrency(currency);
		guestLogin.setLang(lang);
		guestLogin.setJurisdiction(jurisdiction);
		guestLogin.setClientType(clientType);
		guestLogin.setIpAddress(ipAddress);
		guestLogin.setUserAgent(userAgent);
		guestLogin.setAuthToken(authToken);
		return guestLogin;
	}
}
