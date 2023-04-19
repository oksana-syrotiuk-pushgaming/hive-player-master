package io.gsi.hive.platform.player.mesh.player;

import io.gsi.hive.platform.player.mesh.presets.MeshAuthorizationPresets;

public class MeshPlayerTokenBuilder {
	private String type;
	private String token;
	private Integer expires;

	public MeshPlayerTokenBuilder() {
		type = "Bearer";
		token = MeshAuthorizationPresets.DEFAULT_TOKEN;
		expires = 1440;
	}

	public MeshPlayerTokenBuilder withType(String type) {
		this.type = type;
		return this;
	}

	public MeshPlayerTokenBuilder withToken(String token) {
		this.token = token;
		return this;
	}

	public MeshPlayerTokenBuilder withExpires(Integer expires) {
		this.expires = expires;
		return this;
	}

	public MeshPlayerToken get() {
		return new MeshPlayerToken(type, token,expires);
	}
}
