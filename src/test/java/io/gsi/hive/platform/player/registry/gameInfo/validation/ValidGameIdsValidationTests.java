package io.gsi.hive.platform.player.registry.gameInfo.validation;

import io.gsi.hive.platform.player.registry.gameInfo.ValidGameIds;
import org.junit.Test;

import javax.validation.*;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidGameIdsValidationTests {
    @Test
    public void givenNullGameIds_whenValidate_thenConstraintViolationExceptionThrown() {
        ValidGameIds validGameIds = ValidGameIds.builder().build();
        assertThatThrownBy(() -> validate(validGameIds))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("gameCodeToGameId: validator.gameIds.invalid");
    }

    @Test
    public void givenEmptyGameIds_whenValidate_thenConstraintViolationExceptionThrown() {
        ValidGameIds validGameIds = ValidGameIds.builder().gameCodeToGameId(Map.of()).build();
        assertThatThrownBy(() -> validate(validGameIds))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("gameCodeToGameId: validator.gameIds.invalid");
    }

    @Test
    public void givenDuplicateGameId_whenValidate_thenConstraintViolationExceptionThrown() {
        ValidGameIds validGameIds = ValidGameIds.builder().gameCodeToGameId(Map.of("testGame", 1000, "testGame2", 1000)).build();
        assertThatThrownBy(() -> validate(validGameIds))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("gameCodeToGameId: validator.gameIds.invalid");
    }

    @Test
    public void givenValidGameIdConfig_whenValidate_thenNoExceptionThrown() {
        ValidGameIds validGameIds = ValidGameIds.builder().gameCodeToGameId(Map.of("testGame", 1000, "testGame2", 2000)).build();
        validate(validGameIds);
    }

    private <T> void validate(T toValidate) {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(toValidate);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }
}
