/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.player;

import java.io.Serializable;
import java.util.Objects;

/**
 * PlayerKey.  The combination of playerId and igpCode uniquely defines the player
 * on the rhino platform.
 */
@SuppressWarnings("serial")
public class PlayerKey implements Serializable {

	private String playerId;
	private String igpCode;
	private boolean guest;

	protected PlayerKey() {}

	public PlayerKey(String playerId, String igpCode, boolean guest) {
		this.playerId = playerId;
		this.igpCode = igpCode;
		this.guest = guest;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setIgpCode(String igpCode) {
		this.igpCode = igpCode;
	}

	public String getIgpCode() {
		return igpCode;
	}

	public boolean isGuest() {
		return this.guest;
	}

	public void setGuest(boolean guest) {
		this.guest = guest;
	}

	@Override
	public int hashCode() {
		return Objects.hash(playerId,igpCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlayerKey other = (PlayerKey) obj;
		return Objects.equals(this.playerId, other.playerId)
				&& Objects.equals(this.igpCode, other.igpCode);
	}

	@Override
	public String toString() {
		return "PlayerKey [playerId=" + playerId + ", igpCode=" + igpCode + "]";
	}

}
