package io.gsi.hive.platform.player.txn.report;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TxnReportRecordMapper extends TxnReportRecordRowMapper
{
    public TxnReportRecordMapper(final TxnReportArguments reportArguments)
    {
        super(reportArguments);
    }

    @Override
    public TxnReportRecord mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        TxnReportRecord record = super.mapRow(rs,rowNum);
        record.setUniquePlayers(rs.getLong("unique_players"));
        record.setNumPlays(rs.getLong("num_plays"));
        return record;
    }
}
