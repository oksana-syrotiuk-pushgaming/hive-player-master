/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshPlayerToken {

	@NotNull
	private String type;
	@NotNull
	private String token;
	@NotNull
	private Integer expires;

	public MeshPlayerToken() {}

	@JsonCreator
	public MeshPlayerToken(
			@JsonProperty("type") String type,
			@JsonProperty("token") String token,
			@JsonProperty("expires") Integer expires) {
		this.type = type;
		this.token = token;
		this.expires = expires;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Integer getExpires() {
		return expires;
	}

	public void setExpires(Integer expires) {
		this.expires = expires;
	}

	@Override
	public String toString() {
		return "PlayerToken [type=" + type + ", token=" + token + ", expires="
				+ expires + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MeshPlayerToken that = (MeshPlayerToken) o;
		return Objects.equals(type, that.type) &&
				Objects.equals(token, that.token) &&
				Objects.equals(expires, that.expires);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, token, expires);
	}
}
