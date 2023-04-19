package io.gsi.hive.platform.player.txn.search;

import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TxnSearchRecordMapper implements RowMapper<TxnSearchRecord>
{
    @Override
    public TxnSearchRecord mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        TxnSearchRecord txnSearchRecord = new TxnSearchRecord();
        txnSearchRecord.setTxnId(rs.getString("txn_id"));

        txnSearchRecord.setGameCode(rs.getString("game_code"));

        txnSearchRecord.setPlayId(rs.getString("play_id"));

        txnSearchRecord.setPlayComplete(rs.getBoolean("play_complete"));

        txnSearchRecord.setPlayCompleteIfCancelled(
                rs.getBoolean("play_complete_if_cancelled"));

        txnSearchRecord.setRoundId(rs.getString("round_id"));

        txnSearchRecord.setRoundComplete(rs.getBoolean("round_complete"));

        txnSearchRecord.setRoundCompleteIfCancelled(rs.getBoolean("round_complete_if_cancelled"));

        txnSearchRecord.setPlayerId(rs.getString("player_id"));

        txnSearchRecord.setUsername(rs.getString("username"));

        txnSearchRecord.setCountry(rs.getString("country"));

        txnSearchRecord.setIgpCode(rs.getString("igp_code"));

        txnSearchRecord.setSessionId(rs.getString("session_id"));

        txnSearchRecord.setMode(
                Mode.findModeByName(rs.getString("mode")));

        txnSearchRecord.setGuest(rs.getBoolean("guest"));

        txnSearchRecord.setCcyCode(rs.getString("ccy_code"));

        txnSearchRecord.setType(TxnType.findByName(rs.getString("type")));

        txnSearchRecord.setAmount(rs.getBigDecimal("amount"));

        txnSearchRecord.setTxnRef(rs.getString("txn_ref"));

        txnSearchRecord.setStatus(TxnStatus.findByName(
                rs.getString("status")));

        txnSearchRecord.setBonus(rs.getBoolean("bonus"));

        txnSearchRecord.setAccessToken(rs.getString("access_token"));

        // all dates are explicitly retrieved in utc by the query
        Instant txnTs = rs.getTimestamp("txn_ts").toInstant();
        txnSearchRecord.setTxnTs(ZonedDateTime.ofInstant(txnTs,ZoneId.of("UTC")));

        return txnSearchRecord;
    }
}
