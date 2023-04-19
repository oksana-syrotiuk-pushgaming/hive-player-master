package io.gsi.hive.platform.player.session;


import io.gsi.commons.test.validation.ValidatorTester;
import io.gsi.hive.platform.player.builders.SessionBuilder;
import javax.validation.constraints.Size;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static io.gsi.commons.test.string.StringUtils.generateRandomString;

public class SessionValidationTests {

  private ValidatorTester<Session> validatorTester;

  @Before
  public void setup() {
    validatorTester = new ValidatorTester<>();
  }

  @Test
  public void givenSessionWith64CharToken_whenValidate_noErrorsReturned() {
    Session session = SessionBuilder.aSession().build();
    assertThat(validatorTester.validate(session).ok().numberOfErrors())
        .isEqualTo(0);
  }

  @Test
  public void givenSessionWith256CharToken_whenValidate_noErrorsReturned() {
    Session session = SessionBuilder.aSession().withAccessToken(generateRandomString(256)).build();
    assertThat(validatorTester.validate(session).ok().numberOfErrors())
        .isEqualTo(0);
  }

  @Test
  public void givenSessionWith257CharToken_whenValidate_tokenSizeError() {
    Session session = SessionBuilder.aSession().withAccessToken(generateRandomString(257)).build();
    assertThat(validatorTester.validate(session).fails("accessToken", Size.class).numberOfErrors())
        .isEqualTo(1);
  }

}
