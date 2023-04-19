package io.gsi.hive.platform.player.cleardown.report;

import static io.gsi.hive.platform.player.play.report.PlayGroupBy.ccy_code;
import static io.gsi.hive.platform.player.play.report.PlayGroupBy.country;
import static io.gsi.hive.platform.player.play.report.PlayGroupBy.game_code;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class CleardownReportRecordMapper implements RowMapper<CleardownReportRecord> {

  private final CleardownReportArguments reportArguments;

  public CleardownReportRecordMapper(final CleardownReportArguments reportArguments) {
    this.reportArguments = reportArguments;
  }

  @Override
  public CleardownReportRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
    CleardownReportRecord cleardownReportRecord = new CleardownReportRecord();

    if (reportArguments.getGroupBy().contains(game_code)) {
      cleardownReportRecord.setGameCode(rs.getString(game_code.toString()));
    }
    if (reportArguments.getGroupBy().contains(ccy_code)) {
      cleardownReportRecord.setCcyCode(rs.getString("ccy_code"));
    }

    if (reportArguments.getGroupBy().contains(country)) {
      cleardownReportRecord.setCountry(rs.getString(country.toString()));
    }

    cleardownReportRecord.setIgpCode(rs.getString("igp_code"));

    cleardownReportRecord.setUniquePlayers(rs.getLong("unique_players"));
    cleardownReportRecord.setNumCleardowns(rs.getLong("num_cleardowns"));
    cleardownReportRecord.setTotalWin(rs.getBigDecimal("win"));

    return cleardownReportRecord;
  }
}
