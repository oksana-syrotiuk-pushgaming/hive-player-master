package io.gsi.hive.platform.player.registry.gameInfo.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;

public class GameIdsValidator implements ConstraintValidator<GameIds, Map<String, Integer>> {
    @Override
    public boolean isValid(Map<String, Integer> gameCodeToGameId, ConstraintValidatorContext constraintValidatorContext) {
        if (gameCodeToGameId == null) {
            return false;
        }
        long size = gameCodeToGameId.values().size();
        if (size == 0) {
            return false;
        }
        return gameCodeToGameId.values().stream().distinct().count() == size;
    }
}

