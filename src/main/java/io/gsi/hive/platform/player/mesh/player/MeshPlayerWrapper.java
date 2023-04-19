/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Simple wrapper class for a player to include a token
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshPlayerWrapper {

	private MeshPlayer player;
	@JsonInclude(Include.NON_NULL)
	private MeshPlayerToken token;

	public MeshPlayerWrapper() {}

	@JsonCreator
	public MeshPlayerWrapper(
			@JsonProperty("player") MeshPlayer player, 
			@JsonProperty("token") MeshPlayerToken token) {
		this.player = player;
		this.token = token;
	}

	public MeshPlayer getPlayer() {
		return player;
	}

	public void setPlayer(MeshPlayer player) {
		this.player = player;
	}

	public MeshPlayerToken getToken() {
		return token;
	}

	public void setToken(MeshPlayerToken token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return "PlayerWrapper [player=" + player + ", token=" + token + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MeshPlayerWrapper that = (MeshPlayerWrapper) o;
		return Objects.equals(player, that.player) &&
				Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(player, token);
	}
}
