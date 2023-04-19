/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.cleardown.report;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = {CleardownReportArguments.Validator.class})
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CleardownReportArgumentsConstraint {

  String message() default "{constraint.playReport.invalid}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
