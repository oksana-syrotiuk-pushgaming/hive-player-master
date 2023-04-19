package io.gsi.hive.platform.player.player;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import org.junit.Test;

public class PlayerTests extends DomainTestBase {

	@Test
	public void notNullFields() {
		assertThat(numberOfValidationErrors(new Player()), is(5));
	}

	@Test
	public void maximumLengthOfFields() {
		final var player = PlayerBuilder.aPlayer()
				.withIgpCode(randomAlphanumeric(13))
				.withPlayerId(randomAlphanumeric(251))
				.withUsername(randomAlphanumeric(65))
				.withAlias(randomAlphanumeric(65))
				.withCountry(randomAlphanumeric(5))
				.withLang(randomAlphanumeric(5))
				.build();
		assertThat(numberOfValidationErrors(player), is(5));
	}

	@Test
	public void minimumLengthOfFields() {
		final var player = PlayerBuilder.aPlayer()
				.withIgpCode(randomAlphanumeric(0))
				.withPlayerId(randomAlphanumeric(0))
				.withUsername(randomAlphanumeric(0))
				.withAlias(randomAlphanumeric(0))
				.withCountry(randomAlphanumeric(1))
				.withLang(randomAlphanumeric(1))
				.build();
		assertThat(numberOfValidationErrors(player), is(5));
	}
}
