package io.gsi.hive.platform.player.play.search;

import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;

public class PlaySearchArgumentsBuilders {

    public static PlaySearchArguments.PlaySearchArgumentsBuilder defaultPlaySearchArgumentsBuilder() {

        return PlaySearchArguments.builder()
            .playId("1000-1")
            .playerId(PlayerPresets.PLAYERID)
            .ccyCode(PlayerPresets.CCY_CODE)
            .igpCodes(Collections.singletonList(IgpPresets.IGPCODE_IGUANA))
            .dateFrom(ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC")))
            .dateTo(ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC")))
            .guest(Boolean.FALSE);
    }
}
