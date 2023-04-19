package io.gsi.hive.platform.player.session;

import org.hibernate.annotations.DiscriminatorFormula;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.Size;

/**
 * Player session.  Demo and real play sessions are handled.
 */
@Entity(name = "t_session")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when session_token is null then 1 else 2 end")
@DiscriminatorValue("1")
public class Session {

	@Id	@Column(name="session_id")
	private String id;
	@Column(name="creation_time")
	private Long creationTime;
	@Column(name="last_accessed_time")
	private Long lastAccessedTime;
	@Column(name="player_id")
	private String playerId;
	@Column(name="igp_code")
	private String igpCode;
	@Column(name="game_code")
	private String gameCode;
	@Column(name="access_token") @Size(max = 256)
	private String accessToken;
	@Column(name="mode") @Enumerated(EnumType.STRING)
	private Mode mode;
	@Column(name="ccy_code")
	private String ccyCode;
	@Column(name="lang")
	private String lang;
    @Column(name="jurisdiction")
    private String jurisdiction;
	@Column
	private BigDecimal balance;
	@Column(name="authenticated")
	private boolean authenticated;
	@Column(name="status")  @Enumerated(EnumType.STRING)
	private SessionStatus status;
	@Column(name="ip_address")
	private String ipAddress;
	@Size(max = 256)
	@Column(name="user_agent", length = 256)
	private String userAgent;
	@Size(max = 256)
	@Column(name="reason", length = 256)
	private String reason;


	public Session() {
		this.id = UUID.randomUUID().toString();
		this.creationTime = Instant.now().toEpochMilli();
		this.lastAccessedTime = this.creationTime;
		this.authenticated = false;
		this.status = SessionStatus.ACTIVE;
	}

	public String getId() {
		return id;
	}

	public Long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Long creationTime) {
		this.creationTime = creationTime;
	}

	public Long getLastAccessedTime() {
		return lastAccessedTime;
	}

	public void setLastAccessedTime(Long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getIgpCode() {
		return igpCode;
	}

	public void setIgpCode(String igpCode) {
		this.igpCode = igpCode;
	}

	public String getGameCode() {
		return gameCode;
	}

	public void setGameCode(String gameCode) {
		this.gameCode = gameCode;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public String getCcyCode() {
		return ccyCode;
	}

	public void setCcyCode(String ccyCode) {
		this.ccyCode = ccyCode;
	}
	
	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getJurisdiction() { return jurisdiction; }

	public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public boolean isFinished() {
		return status.equals(SessionStatus.FINISHED);
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public SessionStatus getSessionStatus() {
		return status;
	}

	public void setSessionStatus(SessionStatus status) {
		this.status = status;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent.length() > 256 ? userAgent.substring(0,256): userAgent;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason != null && reason.length() > 256 ? reason.substring(0,256): reason;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Session session = (Session) o;
		return authenticated == session.authenticated &&
				Objects.equals(id, session.id) &&
				Objects.equals(creationTime, session.creationTime) &&
				Objects.equals(lastAccessedTime, session.lastAccessedTime) &&
				Objects.equals(playerId, session.playerId) &&
				Objects.equals(igpCode, session.igpCode) &&
				Objects.equals(gameCode, session.gameCode) &&
				Objects.equals(accessToken, session.accessToken) &&
				mode == session.mode &&
				Objects.equals(ccyCode, session.ccyCode) &&
				Objects.equals(lang, session.lang) &&
				Objects.equals(jurisdiction, session.jurisdiction) &&
				Objects.equals(balance, session.balance) &&
				status == session.status &&
				Objects.equals(ipAddress, session.ipAddress) &&
				Objects.equals(userAgent, session.userAgent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, creationTime, lastAccessedTime, playerId, igpCode, gameCode, accessToken, mode, ccyCode, lang, balance, authenticated, status, ipAddress, userAgent);
	}

	@Override
	public String toString() {
		return "Session{" +
				"id='" + id + '\'' +
				", creationTime=" + creationTime +
				", lastAccessedTime=" + lastAccessedTime +
				", playerId='" + playerId + '\'' +
				", igpCode='" + igpCode + '\'' +
				", gameCode='" + gameCode + '\'' +
				", accessToken='" + accessToken + '\'' +
				", mode=" + mode +
				", ccyCode='" + ccyCode + '\'' +
				", lang='" + lang + '\'' +
				", jurisdiction='" + jurisdiction + '\'' +
				", balance=" + balance +
				", authenticated=" + authenticated +
				", status=" + status +
				", ipAddress='" + ipAddress + '\'' +
				", userAgent='" + userAgent + '\'' +
				'}';
	}
}
