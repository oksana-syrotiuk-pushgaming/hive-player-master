package io.gsi.hive.platform.player.txn;

import io.gsi.commons.test.validation.ValidatorTester;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import org.junit.Before;
import org.junit.Test;

import javax.validation.constraints.Size;

import static io.gsi.commons.test.string.StringUtils.generateRandomString;
import static org.assertj.core.api.Assertions.assertThat;

public class TxnValidationTests {

  private ValidatorTester<Txn> txnValidatorTester;

  @Before
  public void setup() {
    txnValidatorTester = new ValidatorTester<>();
  }

  @Test
  public void given64CharToken_whenValidateTxn_noErrors() {
     Txn txn = TxnBuilder.txn().withAccessToken(generateRandomString(64)).build();
     assertThat(txnValidatorTester.validate(txn).ok().numberOfErrors()).isEqualTo(0);
  }

  @Test
  public void given256CharToken_whenValidateTxn_noErrors() {
    Txn txn = TxnBuilder.txn().withAccessToken(generateRandomString(256)).build();
    assertThat(txnValidatorTester.validate(txn).ok().numberOfErrors()).isEqualTo(0);
  }

  @Test
  public void given257CharToken_whenValidateTxn_tokenValidationError() {
    Txn txn = TxnBuilder.txn().withAccessToken(generateRandomString(257)).build();
    assertThat(txnValidatorTester.validate(txn).fails("accessToken", Size.class).numberOfErrors()).isEqualTo(1);
  }
}