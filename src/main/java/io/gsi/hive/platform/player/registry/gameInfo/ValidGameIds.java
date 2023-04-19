package io.gsi.hive.platform.player.registry.gameInfo;

import io.gsi.hive.platform.player.registry.gameInfo.validation.GameIds;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidGameIds {
    @GameIds
    private Map<String, Integer> gameCodeToGameId;
}
