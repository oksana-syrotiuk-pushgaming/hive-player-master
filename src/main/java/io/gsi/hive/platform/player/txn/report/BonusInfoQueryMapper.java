package io.gsi.hive.platform.player.txn.report;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BonusInfoQueryMapper extends TxnReportRecordRowMapper {

	public BonusInfoQueryMapper(final TxnReportArguments reportArguments)
	{
		super(reportArguments);
	}

	@Override
	public TxnReportRecord mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		TxnReportRecord record = super.mapRow(rs,rowNum);
		record.setBonusCost(rs.getBigDecimal("bonusCost"));
		record.setNumFreeRounds(rs.getLong("numFreeRounds"));
		return record;
	}
}
