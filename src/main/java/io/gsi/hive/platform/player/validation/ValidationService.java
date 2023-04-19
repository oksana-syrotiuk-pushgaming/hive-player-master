package io.gsi.hive.platform.player.validation;

import io.gsi.commons.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@Service
public class ValidationService {

	private Validator validator;

	public ValidationService(Validator validator) {
		this.validator = validator;
	}

	/**
	 * validate that domain object is valid, else throw an exception
	 * @param domainObject to validate
	 * @throws ConstraintViolationException if any constraints are violated
	 */
	public <T> void validate(T domainObject) {
		Set<ConstraintViolation<T>> constraintViolations = validator.validate(domainObject);
		if (!constraintViolations.isEmpty()) {
			throw new BadRequestException("Invalid Object Given");
		}
	}
}
