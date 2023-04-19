/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;

import org.junit.Test;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.mesh.presets.MeshAuthorizationPresets;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MeshPlayerWrapperTests extends DomainTestBase
{

	@Test
	public void serialization() {
		MeshPlayerWrapper playerWrapper = new MeshPlayerWrapperBuilder().get();
		String json = jsonMapper.objectToJson(playerWrapper);
		assertThat(getJsonString("$.player.playerId",json),is("player1"));
		assertThat(getJsonString("$.token.token",json),is(MeshAuthorizationPresets.DEFAULT_TOKEN));
	}

	@Test
	public void deserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/PlayerWrapper-valid.json");
		MeshPlayerWrapper playerWrapper = jsonMapper.jsonToObject(json, MeshPlayerWrapper.class);
		assertThat(playerWrapper.getPlayer(),is(notNullValue()));
		assertThat(playerWrapper.getToken(),is(notNullValue()));
	}

}
