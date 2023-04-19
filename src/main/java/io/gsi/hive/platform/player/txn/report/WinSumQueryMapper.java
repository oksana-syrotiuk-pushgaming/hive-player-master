package io.gsi.hive.platform.player.txn.report;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WinSumQueryMapper extends TxnReportRecordRowMapper
{
    public WinSumQueryMapper(final TxnReportArguments reportArguments)
    {
        super(reportArguments);
    }

    @Override
    public TxnReportRecord mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        TxnReportRecord record = super.mapRow(rs,rowNum);
        record.setTotalWin(rs.getBigDecimal("total_win"));
        return record;
    }
}
