package io.gsi.hive.platform.player.txn.report;

import java.sql.ResultSet;
import java.sql.SQLException;


public class StakeSumQueryMapper extends TxnReportRecordRowMapper
{
	public StakeSumQueryMapper(final TxnReportArguments reportArguments)
	{
		super(reportArguments);
	}

	@Override
	public TxnReportRecord mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		TxnReportRecord record = super.mapRow(rs,rowNum);
		record.setTotalStake(rs.getBigDecimal("total_stake"));
		return record;
	}
}
