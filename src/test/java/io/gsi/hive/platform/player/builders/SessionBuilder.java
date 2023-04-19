package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.Session;

import io.gsi.hive.platform.player.session.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public final class SessionBuilder {

	private String id = "session1";
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

	private SessionBuilder() {
	}

	public static SessionBuilder aSession() {
		return new SessionBuilder();
	}

	public SessionBuilder withId(String id) {
		this.id = id;
		return this;
	}

	public SessionBuilder withCreationTime(Long creationTime) {
		this.creationTime = creationTime;
		return this;
	}

	public SessionBuilder withLastAccessedTime(Long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
		return this;
	}

	public SessionBuilder withPlayerId(String playerId) {
		this.playerId = playerId;
		return this;
	}

	public SessionBuilder withIgpCode(String igpCode) {
		this.igpCode = igpCode;
		return this;
	}

	public SessionBuilder withAccessToken(String accessToken) {
		this.accessToken = accessToken;
		return this;
	}

	public SessionBuilder withMode(Mode mode) {
		this.mode = mode;
		return this;
	}

	public SessionBuilder withCcyCode(String ccyCode) {
		this.ccyCode = ccyCode;
		return this;
	}

	public SessionBuilder withLang(String lang) {
		this.lang = lang;
		return this;
	}

	public SessionBuilder withJurisdiction(String jurisdiction) {
		this.jurisdiction = jurisdiction;
		return this;
	}

	public SessionBuilder withBalance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public SessionBuilder withAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
		return this;
	}

	public SessionBuilder withSessionStatus(SessionStatus sessionStatus) {
		this.sessionStatus = sessionStatus;
		return this;
	}

	public Session build() {
		Session session = new Session();
		session.setGameCode(gameCode);
		session.setId(id);
		session.setCreationTime(creationTime);
		session.setLastAccessedTime(lastAccessedTime);
		session.setPlayerId(playerId);
		session.setIgpCode(igpCode);
		session.setAccessToken(accessToken);
		session.setMode(mode);
		session.setCcyCode(ccyCode);
		session.setLang(lang);
		session.setJurisdiction(jurisdiction);
		session.setBalance(balance);
		session.setAuthenticated(authenticated);
		session.setSessionStatus(sessionStatus);
		return session;
	}
}
