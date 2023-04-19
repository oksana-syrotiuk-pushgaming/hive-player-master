/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshPlayerClient {

	@NotNull
	private String ipAddress;
	@NotNull
	private String userAgent;
	public enum Channel {
		UNKNOWN, PC, MOBILE, TABLET, TERMINAL
	}
	@NotNull
	private Channel channel;
	public enum ClientType {
		UNKNOWN, FLASH, HTML, UNITY, NATIVE
	}
	@NotNull
	private ClientType clientType;

	public MeshPlayerClient() {}

	@JsonCreator
	public MeshPlayerClient(
			@JsonProperty("ipAddress") String ipAddress,
			@JsonProperty("userAgent") String userAgent,
			@JsonProperty("channel") Channel channel,
			@JsonProperty("clientType") ClientType clientType) {
		this.ipAddress = ipAddress;
		Optional<String> optUserAgent = Optional.ofNullable(userAgent);
		this.userAgent = optUserAgent.orElse("unknown");
		Optional<Channel> optChannel = Optional.ofNullable(channel);
		this.channel = optChannel.orElse(Channel.UNKNOWN);
		Optional<ClientType> optClientType = Optional.ofNullable(clientType);
		this.clientType = optClientType.orElse(ClientType.UNKNOWN);
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public ClientType getClientType() {
		return clientType;
	}

	public void setClientType(ClientType clientType) {
		this.clientType = clientType;
	}

	@Override
	public String toString() {
		return "PlayerClient [ipAddress=" + ipAddress + ", userAgent="
				+ userAgent + ", channel=" + channel + ", clientType="
				+ clientType + "]";
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MeshPlayerClient that = (MeshPlayerClient) o;
		return Objects.equals(ipAddress, that.ipAddress) &&
				Objects.equals(userAgent, that.userAgent) &&
				channel == that.channel &&
				clientType == that.clientType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ipAddress, userAgent, channel, clientType);
	}
}
