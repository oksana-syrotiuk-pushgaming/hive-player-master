package io.gsi.hive.platform.player.txn.report;

import static io.gsi.hive.platform.player.txn.report.TxnGroupBy.ccy_code;
import static io.gsi.hive.platform.player.txn.report.TxnGroupBy.country;
import static io.gsi.hive.platform.player.txn.report.TxnGroupBy.game_code;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class TxnReportRecordRowMapper implements RowMapper<TxnReportRecord>
{
    private final TxnReportArguments reportArguments;

    public TxnReportRecordRowMapper(final TxnReportArguments reportArguments)
    {
        this.reportArguments = reportArguments;
    }

    @Override
    public TxnReportRecord mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        TxnReportRecord record = new TxnReportRecord();
        Set<TxnGroupBy> groupByColumns = reportArguments.getGroupBy();
        if(groupByColumns.contains(ccy_code))
        {
            record.setCcyCode(rs.getString(ccy_code.toString()));
        }

        if(groupByColumns.contains(country))
        {
            record.setCountry(rs.getString(country.toString()));
        }

        if(groupByColumns.contains(game_code))
        {
            record.setGameCode(rs.getString(game_code.toString()));
        }

        record.setIgpCode(rs.getString("igp_code"));

        return record;
    }
}
