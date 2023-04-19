/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.mesh.player;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.mesh.presets.MeshAuthorizationPresets;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * PlayerTokenTests
 *
 */
public class MeshPlayerTokenTests extends DomainTestBase
{

	@Test
	public void serialization() {
		MeshPlayerToken playerToken = new MeshPlayerTokenBuilder().get();
		String json = jsonMapper.objectToJson(playerToken);
		assertThat(getJsonString("$.type",json),is("Bearer"));
		assertThat(getJsonString("$.token",json),is(MeshAuthorizationPresets.DEFAULT_TOKEN));
		assertThat(getJsonInteger("$.expires",json),is(1440));
	}

	@Test
	public void deserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/PlayerToken-valid.json");
		MeshPlayerToken playerToken = jsonMapper.jsonToObject(json, MeshPlayerToken.class);
		assertThat(playerToken.getType(),is("Bearer"));
		assertThat(playerToken.getToken(),is("abcdefghijkl"));
		assertThat(playerToken.getExpires(),is(1800));
	}

	@Test
	public void unknownFieldIgnoredOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/PlayerToken-ignoreProperties.json");
		MeshPlayerToken playerToken = jsonMapper.jsonToObject(json, MeshPlayerToken.class);
		assertThat(playerToken.getToken(),is("abcdefghijkl"));
	}

	@Test
	public void notNullFields() {
		MeshPlayerToken playerToken = new MeshPlayerTokenBuilder().get();
		ReflectionTestUtils.setField(playerToken, "type", null);
		ReflectionTestUtils.setField(playerToken, "token", null);
		ReflectionTestUtils.setField(playerToken, "expires", null);
		assertThat(numberOfValidationErrors(playerToken),is(3));
	}

}
