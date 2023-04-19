/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.player;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.gsi.hive.platform.player.DomainTestBase;

import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MeshPlayerTests extends DomainTestBase {

	private static final String CHAR_STR_65 = randomAlphanumeric(65);
	private static final String CHAR_STR_251 = randomAlphanumeric(251);

	@Test
	public void successfulSerialization() {
		MeshPlayer player = new MeshPlayerBuilder().get();
		String json = jsonMapper.objectToJson(player);
		assertThat(getJsonString("$.playerId",json),is("player1"));
	}

	@Test
	public void successfulDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/Player-valid.json");
		MeshPlayer player = jsonMapper.jsonToObject(json, MeshPlayer.class);
		assertThat(player.getPlayerId(),is("player1"));
		assertThat(player.getUsername(),is("johndoe"));
		assertThat(player.getAlias(),is("JohnDoe"));
		assertThat(player.getWallet().getBalance(),is(new BigDecimal("110.00")));
		assertThat(player.getWallet().getFunds().size(),is(2));
	}

	@Test
	public void unknownFieldIgnoredOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/player/Player-ignoreProperties.json");
		MeshPlayer player = jsonMapper.jsonToObject(json, MeshPlayer.class);
		assertThat(player.getPlayerId(),is("1"));
	}

	@Test
	public void notNullFields() {
		MeshPlayer player = new MeshPlayerBuilder().get();
		ReflectionTestUtils.setField(player, "playerId", null);
		ReflectionTestUtils.setField(player, "username", null);
		ReflectionTestUtils.setField(player, "alias", null);
		ReflectionTestUtils.setField(player, "country", null);
		ReflectionTestUtils.setField(player, "lang", null);
		ReflectionTestUtils.setField(player, "wallet", null);
		assertThat(numberOfValidationErrors(player),is(4));
	}

	@Test
	public void maximumLengthOfFields() {
		MeshPlayer player = new MeshPlayerBuilder().get();
		ReflectionTestUtils.setField(player, "playerId", CHAR_STR_251);
		ReflectionTestUtils.setField(player, "username", CHAR_STR_65);
		ReflectionTestUtils.setField(player, "alias", CHAR_STR_65);
		ReflectionTestUtils.setField(player, "country", CHAR_STR_65);
		ReflectionTestUtils.setField(player, "lang", CHAR_STR_65);
		assertThat(numberOfValidationErrors(player),is(5));
	}
}
