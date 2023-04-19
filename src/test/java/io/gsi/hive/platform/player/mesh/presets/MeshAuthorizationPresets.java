package io.gsi.hive.platform.player.mesh.presets;

public interface MeshAuthorizationPresets {
	String DEFAULT = "Bearer token";
	String DEFAULT_TOKEN = "token";
	String ACCESS_TOKEN = "altToken";
	String NOTOKEN = "Bearer ";
	String INVALIDTOKEN = "Bearer invalidToken";
	String INVALIDTYPE = "Bear token"; //Thats, not a token, thats a bear
}
