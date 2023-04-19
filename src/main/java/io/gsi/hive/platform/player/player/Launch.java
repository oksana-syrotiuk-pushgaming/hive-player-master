package io.gsi.hive.platform.player.player;

import io.gsi.hive.platform.player.session.Mode;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

// TODO add in gameCode

/**
 * Launch properties.  These are provided by the Launch url parameters and retrieved from the user agent on game
 * launch.
 */
public class Launch {

	@NotEmpty
	private String igpCode;
	private String siteId;
	private String playerId;
	private String accessToken;
	@NotNull
	private Boolean guest;
	@NotNull
	private Mode mode;
	private String lang;
	private String ccyCode;
	private String gameCode;
	private String loginUrl;
	private String accountUrl;
	private String homeUrl;
	@Min(1)
	private Integer realityCheckMins;
	
	@NotEmpty
	private String ipAddress;
	@NotEmpty
	private String userAgent;

	/**
	 * @return operator igp code
	 */
	public String getIgpCode() {
		return igpCode;
	}

	/**
	 *
	 * @param igpCode operator igp code
	 */
	public void setIgpCode(String igpCode) {
		this.igpCode = igpCode;
	}

	/**
	 * @return site identifier if specified on launch
	 */
	public String getSiteId() {
		return siteId;
	}

	/**
	 * @param siteId site identifier
	 */
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	/**
	 * @return playerId if specified on launch
	 */
	public String getPlayerId() {
		return playerId;
	}

	/**
	 * @param playerId igp player identifier
	 */
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	/**
	 * @return authentication token. only present for non guest launch
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * @param accessToken authentication token to authenticate the iGP
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
	 * @return is the player a guest
	 */
	public Boolean isGuest() {
		return guest;
	}

	/**
	 * @param guest is the player a guest
	 */
	public void setGuest(Boolean guest) {
		this.guest = guest;
	}

	/**
	 * @return demo or real play
	 */
	public Mode getMode() {
		return mode;
	}

	/**
	 * @param mode demo or real play
	 */
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	/**
	 * @return language to display the game in.  specified by launch params or defaults to configured default
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * @param lang language to display the game in
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * @return ip address of game client
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param ipAddress ip address of game client
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * @return user agent of game client
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * @param userAgent user agent of game client
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * @return 3 - 6 letter currency to play game in.  Only relevant for demo play
	 */
	public String getCcyCode() {
		return ccyCode;
	}
	/**
	 * @param ccyCode 3 - 6 letter currency to play game in.  Only relevant for demo play
	 */
	public void setCcyCode(String ccyCode) {
		this.ccyCode = ccyCode;
	}

	/**
	 * @return gameCode e.g. cointoss
	 */
	public String getGameCode() {
		return gameCode;
	}

	/**
	 * @param gameCode game code e.g. cointoss
	 */
	public void setGameCode(String gameCode) {
		this.gameCode = gameCode;
	}

	/**
	 * @return login url to redirect to on login links or login errors. can be null.  overrides site/igp settings.
	 */
	public String getLoginUrl() {
		return loginUrl;
	}

	/**
	 * @param loginUrl login url to redirect to on login links or login errors. can be null.  overrides site/igp settings.
	 */
	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	/**
	 * @return accountUrl to redirect to on account links or account errors. can be null. overrides site/igp settings.
	 */
	public String getAccountUrl() {
		return accountUrl;
	}

	/**
	 * @param accountUrl account url to redirect to on account links or account errors. can be null. overrides site/igp settings.
	 */
	public void setAccountUrl(String accountUrl) {
		this.accountUrl = accountUrl;
	}

	/**
	 * @return homeUrl to redirect to on home link or unknown errors. can be null. overrides site/igp settings.
	 */
	public String getHomeUrl() {
		return homeUrl;
	}

	/**
	 * @param homeUrl homeUrl to redirect to on home link or unknown errors. can be null. overrides site/igp settings.
	 */
	public void setHomeUrl(String homeUrl) {
		this.homeUrl = homeUrl;
	}

	/**
	 * @return number of minutes until reality check dialog should be displayed (for UKGC). can be null
	 */
	public Integer getRealityCheckMins() {
		return realityCheckMins;
	}

	/**
	 * @param realityCheckMins number of minutes until reality check dialog should be displayed (for UKGC). can be null
	 */
	public void setRealityCheckMins(Integer realityCheckMins) {
		this.realityCheckMins = realityCheckMins;
	}
}
