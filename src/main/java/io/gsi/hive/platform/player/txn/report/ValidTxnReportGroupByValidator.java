package io.gsi.hive.platform.player.txn.report;

import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidTxnReportGroupByValidator implements ConstraintValidator<ValidTxnReportGroupBy, Set<TxnGroupBy>> {
	@Override
	public void initialize(ValidTxnReportGroupBy constraintAnnotation) {
	}

	@Override
	public boolean isValid(Set<TxnGroupBy> groupBy, ConstraintValidatorContext context) {
		return groupBy.contains(TxnGroupBy.ccy_code);
	}

}
