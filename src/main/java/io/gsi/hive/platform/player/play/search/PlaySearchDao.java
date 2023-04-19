package io.gsi.hive.platform.player.play.search;

import io.gsi.hive.platform.player.txn.search.TxnSearchDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

@Repository
public class PlaySearchDao {
    private static final Logger logger = LoggerFactory.getLogger(TxnSearchDao.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PlaySearchDao(
        @Qualifier("reportNamedParameterJdbcTemplate")
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
    }

    public Long recordCount(PlaySearchArguments searchArguments)
    {
        String countQuery = PlaySearchSqlBuilder
                .aPlaySearchSqlBuilder(searchArguments)
                .buildCountQuery();

        logger.info(countQuery);

        return this.jdbcTemplate.queryForObject(
                countQuery, populateSqlParameterMap(searchArguments),
                new RowMapper<Long>()
                {
                    @Override
                    public Long mapRow(ResultSet rs, int rowNum) throws SQLException
                    {
                        return rs.getLong("total");
                    }
                }
        );
    }

    public List<PlaySearchRecord> search(PlaySearchArguments searchArguments)
    {
        String searchQuery = PlaySearchSqlBuilder
                .aPlaySearchSqlBuilder(searchArguments)
                .buildSearchQuery();

        logger.info(searchQuery);

        return this.jdbcTemplate.query(
                searchQuery, populateSqlParameterMap(searchArguments), new PlaySearchRecordMapper()
        );
    }

    private SqlParameterSource populateSqlParameterMap(PlaySearchArguments searchArguments)
    {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        Timestamp dateFrom = Timestamp.from(searchArguments.getDateFrom().toInstant());
        parameterSource.addValue("dateFrom", dateFrom, Types.TIMESTAMP);

        Timestamp dateTo = Timestamp.from(searchArguments.getDateTo().toInstant());
        parameterSource.addValue("dateTo", dateTo, Types.TIMESTAMP);

        parameterSource.addValue("limit",searchArguments.getPageSize());
        parameterSource.addValue("offset",searchArguments.getPage());
        parameterSource.addValue("igpCodes",searchArguments.getIgpCodes());
        parameterSource.addValue("mode",searchArguments.getMode().name());
        parameterSource.addValue("guest", searchArguments.getGuest());

        if (searchArguments.getPlayId() != null) {
            parameterSource.addValue("playId", searchArguments.getPlayId());
        }

        if (searchArguments.getCcyCode() != null) {
            parameterSource.addValue("ccyCode", searchArguments.getCcyCode());
        }

        if (searchArguments.getPlayerId() != null) {
            parameterSource.addValue("playerId", searchArguments.getPlayerId());
        }

        if (searchArguments.getGameCode() != null) {
            parameterSource.addValue("gameCode", searchArguments.getGameCode());
        }

        if (searchArguments.getStatus() != null) {
            parameterSource.addValue("status", searchArguments.getStatus().name());
        }

        if (searchArguments.getPlayerId() != null) {
            parameterSource.addValue("playerId", searchArguments.getPlayerId());
        }

        if (searchArguments.getPlayRef() != null) {
            parameterSource.addValue("playRef", searchArguments.getPlayRef());
        }

        return parameterSource;
    }
}
