package io.gsi.hive.platform.player.mesh.player;

public class MeshPlayerWrapperBuilder {
	
	MeshPlayer player;
	MeshPlayerToken playerToken; 
	
	MeshPlayerBuilder playerBuilder;
	MeshPlayerTokenBuilder playerTokenBuilder;
	
	public MeshPlayerWrapperBuilder withPlayer(MeshPlayer player)
	{
		this.player = player;
		return this;
	}
	
	public MeshPlayerWrapperBuilder withPlayerToken(MeshPlayerToken playerToken)
	{
		this.playerToken = playerToken;
		return this;
	}
	
	public MeshPlayerWrapperBuilder ()
	{
		playerBuilder = new MeshPlayerBuilder();
		playerTokenBuilder = new MeshPlayerTokenBuilder();
		
		this.player = playerBuilder.get();
		this.playerToken = playerTokenBuilder.get();
	}
	
	public MeshPlayerWrapper get()
	{
		return new MeshPlayerWrapper(player, playerToken);
	}
	
}
