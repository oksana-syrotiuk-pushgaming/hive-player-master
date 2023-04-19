package io.gsi.hive.platform.player.txn;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Test;

import io.gsi.hive.platform.player.txn.event.TxnRequest;

public class TxnRequestValidationTests {

	private final Validator validator;

	public TxnRequestValidationTests() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	public void ok() {
		TxnRequest txnRequest = defaultWinTxnRequestBuilder().build();

		assertThat(validator.validate(txnRequest).isEmpty()).isTrue();
	}
	
	@Test
	public void invalidNullTxnId() {
		TxnRequest gameTxn = defaultWinTxnRequestBuilder().txnId(null).build();

		assertThat(validator.validate(gameTxn).size()).isEqualTo(1);
	}
	
	@Test
	public void invalidBlankGameCode() {
		TxnRequest gameTxn = defaultWinTxnRequestBuilder().gameCode(" ").build();

		assertThat(validator.validate(gameTxn).size()).isEqualTo(1);
	}
	
	@Test 
	public void invalidNegativeAmount() {
		TxnRequest gameTxn = defaultWinTxnRequestBuilder()
				.amount(new BigDecimal(-1))
				.build();

		assertThat(validator.validate(gameTxn).size()).isEqualTo(1);
	}
}
