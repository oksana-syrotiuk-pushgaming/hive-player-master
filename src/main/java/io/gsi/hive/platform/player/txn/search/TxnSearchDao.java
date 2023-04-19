package io.gsi.hive.platform.player.txn.search;

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
public class TxnSearchDao
{
    private static final Logger logger = LoggerFactory.getLogger(TxnSearchDao.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TxnSearchDao(
        @Qualifier("reportNamedParameterJdbcTemplateLongerDefaultTimeout")
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
    }

    public Long recordCount(TxnSearchArguments searchArguments)
    {
        String countQuery = TxnSearchSqlBuilder
                .aTxnSearchSqlBuilder(searchArguments)
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

    public List<TxnSearchRecord> search(TxnSearchArguments searchArguments)
    {
        String searchQuery = TxnSearchSqlBuilder
                .aTxnSearchSqlBuilder(searchArguments)
                .buildSearchQuery();

        logger.info(searchQuery);

        return this.jdbcTemplate.query(
                searchQuery, populateSqlParameterMap(searchArguments), new TxnSearchRecordMapper()
        );
    }

    private SqlParameterSource populateSqlParameterMap(TxnSearchArguments searchArguments)
    {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        Timestamp dateFrom = Timestamp.from(searchArguments.getDateFrom().toInstant());
        parameterSource.addValue("dateFrom", dateFrom, Types.TIMESTAMP);

        Timestamp dateTo = Timestamp.from(searchArguments.getDateTo().toInstant());
        parameterSource.addValue("dateTo", dateTo, Types.TIMESTAMP);

        parameterSource.addValue("guest", searchArguments.isGuest());
        parameterSource.addValue("limit",searchArguments.getPageSize());
        parameterSource.addValue("offset",searchArguments.getPage());
        parameterSource.addValue("igpCodes", searchArguments.getIgpCodes());

        if (searchArguments.getType() != null) {
            parameterSource.addValue("type", searchArguments.getType().name());
        }

        if (searchArguments.getPlayerId() != null) {
            parameterSource.addValue("playerId", searchArguments.getPlayerId());
        }

        if (searchArguments.getUsername() != null) {
            parameterSource.addValue("username", searchArguments.getUsername());
        }

        if (searchArguments.getGameCode() != null) {
            parameterSource.addValue("gameCode", searchArguments.getGameCode());
        }

        if (searchArguments.getMode() != null) {
            parameterSource.addValue("mode", searchArguments.getMode().name());
        }

        if (searchArguments.getStatus() != null) {
            parameterSource.addValue("status", searchArguments.getStatus().name());
        }

        if (searchArguments.getCcyCode() != null) {
            parameterSource.addValue("ccyCode", searchArguments.getCcyCode());
        }

        if (searchArguments.getCountry() != null) {
            parameterSource.addValue("country", searchArguments.getCountry());
        }

        if (searchArguments.getTxnId() != null) {
            parameterSource.addValue("txnId", searchArguments.getTxnId());
        }

        if (searchArguments.getPlayId() != null) {
            parameterSource.addValue("playId", searchArguments.getPlayId());
        }

        if (searchArguments.getPlayRef() != null) {
            parameterSource.addValue("playRef", searchArguments.getPlayRef());
        }

        if (searchArguments.getTxnRef() != null) {
            parameterSource.addValue("txnRef", searchArguments.getTxnRef());
        }

        if (searchArguments.getAccessToken() != null) {
            parameterSource.addValue("accessToken", searchArguments.getAccessToken());
        }

        return parameterSource;
    }

}
