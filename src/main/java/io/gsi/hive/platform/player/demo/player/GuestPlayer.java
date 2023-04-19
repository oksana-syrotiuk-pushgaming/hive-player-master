/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.demo.player;

import javax.validation.constraints.NotNull;
import java.util.Objects;

public class GuestPlayer {
	@NotNull
	private String playerId;
	@NotNull
	private String igpCode;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GuestPlayer that = (GuestPlayer) o;
		return Objects.equals(playerId, that.playerId) &&
				Objects.equals(igpCode, that.igpCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(playerId, igpCode);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("GuestPlayer{");
		sb.append("playerId='").append(playerId).append('\'');
		sb.append(", igpCode='").append(igpCode).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
