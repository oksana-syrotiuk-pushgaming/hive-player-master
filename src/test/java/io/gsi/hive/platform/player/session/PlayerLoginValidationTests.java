package io.gsi.hive.platform.player.session;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Test;

import io.gsi.hive.platform.player.builders.PlayerLoginBuilder;

public class PlayerLoginValidationTests {

	private final Validator validator;

	public PlayerLoginValidationTests()
	{
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	public void ok()
	{
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();
		
		assertThat(validator.validate(playerLogin).isEmpty()).isTrue();
	}

	@Test
	public void invalidNullClientType()
	{
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().withClientType(null).build();
 
		assertThat(validator.validate(playerLogin).size()).isEqualTo(1);
	}

	@Test
	public void invalidLang()
	{
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().withLang("english").build();

		assertThat(validator.validate(playerLogin).size()).isEqualTo(1);
	}
}
