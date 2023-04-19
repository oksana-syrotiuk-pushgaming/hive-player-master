package io.gsi.hive.platform.player.play.report;

import static io.gsi.hive.platform.player.play.report.PlayGroupBy.ccy_code;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.session.Mode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;

public class PlayReportArgumentsBuilders {

  public static PlayReportArguments.PlayReportArgumentsBuilder defaultPlayReportArgumentsBuilder() {
      return PlayReportArguments.builder()
          .groupBy(Collections.singleton(ccy_code))
          .playerId(PlayerPresets.PLAYERID)
          .ccyCode(PlayerPresets.CCY_CODE)
          .gameCode(GamePresets.CODE)
          .guest(false)
          .mode(Mode.real)
          .dateFrom(ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC")))
          .dateTo(ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC")));
  }
}
