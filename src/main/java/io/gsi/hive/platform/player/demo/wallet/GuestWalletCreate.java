/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.demo.wallet;

import java.time.Instant;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GuestWalletCreate {

	@NotNull
	private String type;
	@NotNull
	private Long timestamp;

	public GuestWalletCreate() {
		type="walletCreate";
		timestamp = Instant.now().toEpochMilli();
	}

	@NotNull @Size(min=1,max=250)
	private String playerId;
	@NotNull @Size(min=1,max=32)
	private String igpCode;
	@NotNull
	private Boolean guest;
	@NotNull @Size(min=1,max=64)
	private String gameCode;
	@NotNull
	private String ccyCode;

	public String getType() {
		return type;
	}

	public Long getTimestamp() {
		return timestamp;
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

	public Boolean getGuest() {
		return guest;
	}

	public void setGuest(Boolean guest) {
		this.guest = guest;
	}

	public String getGameCode() {
		return gameCode;
	}

	public void setGameCode(String gameCode) {
		this.gameCode = gameCode;
	}

	public String getCcyCode() {
		return ccyCode;
	}

	public void setCcyCode(String ccyCode) {
		this.ccyCode = ccyCode;
	}

	@Override
	public String toString() {
		return "GuestWalletCreate{" +
				"type='" + type + '\'' +
				", timestamp=" + timestamp +
				", playerId='" + playerId + '\'' +
				", igpCode='" + igpCode + '\'' +
				", guest=" + guest +
				", gameCode='" + gameCode + '\'' +
				", ccyCode='" + ccyCode + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GuestWalletCreate that = (GuestWalletCreate) o;
		return Objects.equals(type, that.type) &&
				Objects.equals(timestamp, that.timestamp) &&
				Objects.equals(playerId, that.playerId) &&
				Objects.equals(igpCode, that.igpCode) &&
				Objects.equals(guest, that.guest) &&
				Objects.equals(gameCode, that.gameCode) &&
				Objects.equals(ccyCode, that.ccyCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, timestamp, playerId, igpCode, guest, gameCode, ccyCode);
	}
}
