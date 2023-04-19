package io.gsi.hive.platform.player.play.search;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.jdbc.core.RowMapper;

import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.session.Mode;

public class PlaySearchRecordMapper implements RowMapper<PlaySearchRecord>
{
    @Override
    public PlaySearchRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        PlaySearchRecord playSearchRecord = new PlaySearchRecord();
        playSearchRecord.setPlayId(rs.getString("play_id"));

        playSearchRecord.setPlayerId(rs.getString("player_id"));

        playSearchRecord.setGameCode(rs.getString("game_code"));

        playSearchRecord.setModifiedAt(ZonedDateTime.ofInstant(rs.getTimestamp("modified_at").toInstant(),
                ZoneId.of("UTC")));        
        playSearchRecord.setCreatedAt(ZonedDateTime.ofInstant(rs.getTimestamp("created_at").toInstant(),
                ZoneId.of("UTC")));

        playSearchRecord.setIgpCode(rs.getString("igp_code"));

        playSearchRecord.setNumTxns(rs.getInt("num_txns"));

        playSearchRecord.setMode(
                Mode.findModeByName(rs.getString("mode")));

        playSearchRecord.setGuest(rs.getBoolean("guest"));

        playSearchRecord.setCcyCode(rs.getString("ccy_code"));

        playSearchRecord.setWin(rs.getBigDecimal("win"));

        playSearchRecord.setStake(rs.getBigDecimal("stake"));

        playSearchRecord.setCountry(rs.getString("country"));

        playSearchRecord.setIsFreeRound(rs.getBoolean("is_free_round"));

        playSearchRecord.setStatus(PlayStatus.findByName(
                rs.getString("status")));

        playSearchRecord.setPlayRef(rs.getString("play_ref"));

        return playSearchRecord;
    }
}
