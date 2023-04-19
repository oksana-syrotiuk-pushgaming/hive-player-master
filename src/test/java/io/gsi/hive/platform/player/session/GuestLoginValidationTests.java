package io.gsi.hive.platform.player.session;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Test;

import io.gsi.hive.platform.player.builders.GuestLoginBuilder;

public class GuestLoginValidationTests {

	private Validator validator;

	public GuestLoginValidationTests()
	{
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	public void ok()
	{
		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().build();
		
		assertThat(validator.validate(guestLogin).isEmpty()).isTrue();
	}
	
	@Test
	public void invalidLang()
	{
		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().withLang("english").build();
 
		assertThat(validator.validate(guestLogin).size()).isEqualTo(1);
	}	
	
	@Test
	public void blankUserAgent()
	{
		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().withUserAgent(" ").build();
 
		assertThat(validator.validate(guestLogin).size()).isEqualTo(1);
	}	
}
