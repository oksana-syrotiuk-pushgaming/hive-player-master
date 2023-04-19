package io.gsi.hive.platform.player.mesh.player;

import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient.Channel;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient.ClientType;
import io.gsi.hive.platform.player.mesh.presets.MeshIpAddressPresets;
import io.gsi.hive.platform.player.mesh.presets.MeshUserAgentPresets;

public class MeshPlayerClientBuilder 
{
	private String ipAddress = MeshIpAddressPresets.DEFAULT;
	private String userAgent = MeshUserAgentPresets.DEFAULT;
	private Channel channel;
	private ClientType clientType;
	
	public MeshPlayerClientBuilder ()
	{
		this.channel = Channel.PC;
		this.clientType = ClientType.HTML;
	}
	
	public MeshPlayerClientBuilder withIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
		return this;
	}
	
	public MeshPlayerClientBuilder withUserAgent(String userAgent)
	{
		this.userAgent = userAgent;
		return this;
	}
	
	public MeshPlayerClientBuilder withChannel(Channel channel)
	{
		this.channel = channel;
		return this;
	}
	
	public MeshPlayerClientBuilder withClientType(ClientType clientType)
	{
		this.clientType = clientType;
		return this;
	}
	
	public MeshPlayerClient get()
	{
		return new MeshPlayerClient(ipAddress, userAgent, channel, clientType);
	}
	
}
