package io.gsi.hive.platform.player.play.report;

import static io.gsi.hive.platform.player.play.report.PlayGroupBy.ccy_code;
import static io.gsi.hive.platform.player.play.report.PlayGroupBy.country;
import static io.gsi.hive.platform.player.play.report.PlayGroupBy.game_code;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class PlayReportRecordMapper implements RowMapper<PlayReportRecord>
{
	private final PlayReportArguments reportArguments;

	public PlayReportRecordMapper(final PlayReportArguments reportArguments)
	{
		this.reportArguments = reportArguments;
	}

	@Override
	public PlayReportRecord mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		PlayReportRecord playReportRecord = new PlayReportRecord();


		if(reportArguments.getGroupBy().contains(game_code)) {
			playReportRecord.setGameCode(rs.getString(game_code.toString()));
		}
		if(reportArguments.getGroupBy().contains(ccy_code)) {
			playReportRecord.setCcyCode(rs.getString("ccy_code"));
		}

		if(reportArguments.getGroupBy().contains(country)) {
			playReportRecord.setCountry(rs.getString(country.toString()));
		}

		playReportRecord.setIgpCode(rs.getString("igp_code"));

		playReportRecord.setUniquePlayers(rs.getLong("unique_players"));
		playReportRecord.setNumPlays(rs.getLong("num_plays"));
		playReportRecord.setTotalStake(rs.getBigDecimal("stake"));
		playReportRecord.setTotalWin(rs.getBigDecimal("win"));
		playReportRecord.setGrossGamingRevenue(rs.getBigDecimal("gross_gaming_revenue"));

		return playReportRecord;
	}
}
