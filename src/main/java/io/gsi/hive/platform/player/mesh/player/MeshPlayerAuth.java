/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;


import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringTokenizer;

import io.gsi.commons.exception.AuthorizationException;

/**
 * Represents a player authorization credential
 */
public class MeshPlayerAuth {

	private String type;
	private String token;

	public MeshPlayerAuth(String type, String token) {
		if (!type.contentEquals("Bearer")) {
			throw new AuthorizationException("Invalid authorization scheme");
		}
		this.type = type;
		this.token = token;
	}

	public MeshPlayerAuth(String token) {
		this.type = "Bearer";
		this.token = token;
	}

	public static MeshPlayerAuth constructFromHeader(String authHeader) {
		if (authHeader == null || authHeader.length() == 0) {
			return new MeshPlayerAuth(null);
		}
		StringTokenizer st = new StringTokenizer(authHeader);
		String scheme = st.nextToken();
		if (!scheme.contentEquals("Bearer")) {
			throw new AuthorizationException("Invalid authorization scheme");
		}
		String token = null;
		try {
			token = st.nextToken();
		} catch (NoSuchElementException e) {
			throw new AuthorizationException("Missing token");
		}
		return new MeshPlayerAuth(scheme,token);
	}

	public String getHeader() {
		return type + " " + token;
	}

	public boolean valid() {
		if (token == null || token.length() == 0) {
			return false;
		}
		return true;
	}

	public String getType() {
		return type;
	}

	public String getToken() {
		return token;
	}

	@Override
	public String toString() {
		return "PlayerAuth [type=" + type + ", token=" + token + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MeshPlayerAuth that = (MeshPlayerAuth) o;
		return Objects.equals(type, that.type) &&
				Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, token);
	}
}
