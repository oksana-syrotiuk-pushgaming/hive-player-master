package io.gsi.hive.platform.player.mesh.player;

import io.gsi.hive.platform.player.mesh.presets.MeshAuthorizationPresets;

public class MeshPlayerAuthBuilder {
/*	private PlayerAuth playerAuth;

	public PlayerAuthBuilder() {
		this.playerAuth = createWithDefaults();
	}

	public PlayerAuth get() {
		return playerAuth;
	}
	
	private PlayerAuth createWithDefaults() {
		return new PlayerAuth("Bearer", "token");
	}*/
	
	
	private String token = MeshAuthorizationPresets.DEFAULT_TOKEN;
	private String type = "Bearer";
			
	public MeshPlayerAuthBuilder withToken(String token)
	{
		this.token = token;
		return this;
	}
			
	public MeshPlayerAuth get()
	{
		return new MeshPlayerAuth(type, token);
	}
	
}
