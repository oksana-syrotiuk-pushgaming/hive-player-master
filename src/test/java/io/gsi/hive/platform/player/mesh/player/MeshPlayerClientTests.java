/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient.Channel;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient.ClientType;
import io.gsi.hive.platform.player.mesh.presets.MeshIpAddressPresets;
import io.gsi.hive.platform.player.mesh.presets.MeshUserAgentPresets;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MeshPlayerClientTests extends DomainTestBase
{

	@Test
	public void nullFieldsThrowError() {
		MeshPlayerClient client = new MeshPlayerClientBuilder()
				.withIpAddress(null)
				.get();
		assertThat(numberOfValidationErrors(client),is(1));
	}

	//@Test TODO
	public void invalidIPAddressThrowsError() {
		
	}

	// TODO, test for cross-site scripting in the user-agent string

	@Test
	public void unknownFieldIgnoredOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/PlayerClient-ignoreProperties.json");
		MeshPlayerClient client = jsonMapper.jsonToObject(json, MeshPlayerClient.class);
		assertThat(client.getIpAddress(),is(MeshIpAddressPresets.DEFAULT));
	}

	@Test
	public void defaultChannelAndClientType() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/PlayerClient-missingFields.json");
		MeshPlayerClient client = jsonMapper.jsonToObject(json, MeshPlayerClient.class);
		assertThat(client.getChannel(),is(Channel.UNKNOWN));
		assertThat(client.getClientType(),is(ClientType.UNKNOWN));
	}

	@Test
	public void invalidClientType() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/PlayerClient-invalidClientType.json");
		thrown.expect(IllegalArgumentException.class);
		jsonMapper.jsonToObject(json, MeshPlayerClient.class);
	}

	@Test
	public void successfulSerialization() {
		MeshPlayerClient client = new MeshPlayerClientBuilder().get();
		String json = jsonMapper.objectToJson(client);
		assertThat(getJsonString("$.ipAddress",json),is(MeshIpAddressPresets.DEFAULT));
		assertThat(getJsonString("$.userAgent",json),is(MeshUserAgentPresets.DEFAULT));
		assertThat(getJsonString("$.channel",json),is(Channel.PC.name()));
		assertThat(getJsonString("$.clientType",json),is(ClientType.HTML.name()));
	}

}
