package io.gsi.hive.platform.player.txn.report;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = ValidTxnReportGroupByValidator.class)
public @interface ValidTxnReportGroupBy {

    String message() default "{ValidIsoCountry.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
}