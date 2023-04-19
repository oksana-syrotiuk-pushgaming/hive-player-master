package io.gsi.hive.platform.player.play.report;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class PlayReportDao {
	
    private static final Logger logger = LoggerFactory.getLogger(PlayReportDao.class);
	
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public PlayReportDao(
        @Qualifier("reportNamedParameterJdbcTemplateLongerDefaultTimeout")
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public List<PlayReportRecord> reportQuery(PlayReportArguments playReportArguments)
    {
        PlayReportSqlBuilder sqlBuilder = PlayReportSqlBuilder
            .aPlayReportSqlBuilder(playReportArguments);
            SqlParameterSource parameterSource = populateSqlParameterMap(playReportArguments);

        String reportQuery = sqlBuilder.buildReportQuery();

        logger.info(reportQuery);

        return this.namedParameterJdbcTemplate.query(
                reportQuery, parameterSource, new PlayReportRecordMapper(playReportArguments));

    }

    private SqlParameterSource populateSqlParameterMap(PlayReportArguments reportArguments)
    {
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        Timestamp dateFrom = Timestamp.from(reportArguments.getDateFrom().toInstant());
        parameterSource.addValue("dateFrom", dateFrom, Types.TIMESTAMP);

        Timestamp dateTo = Timestamp.from(reportArguments.getDateTo().toInstant());
        parameterSource.addValue("dateTo", dateTo, Types.TIMESTAMP);
        parameterSource.addValue("mode",reportArguments.getMode().name());
        parameterSource.addValue("guest", reportArguments.getGuest());
        parameterSource.addValue("igpCodes", reportArguments.getIgpCodes());

        if(reportArguments.getCcyCode() != null)
        {
            parameterSource.addValue("ccyCode", reportArguments.getCcyCode());
        }


        if(reportArguments.getPlayerId() != null)
        {
            parameterSource.addValue("playerId", reportArguments.getPlayerId());
        }


        if(reportArguments.getGameCode() != null)
        {
            parameterSource.addValue("gameCode", reportArguments.getGameCode());
        }

        if(reportArguments.getStatus() != null)
        {
            parameterSource.addValue("status", reportArguments.getStatus().name());
        }

        if(reportArguments.getPlayerId() != null)
        {
            parameterSource.addValue("playerId", reportArguments.getPlayerId());
        }

        if(reportArguments.getCountry() != null) {
            parameterSource.addValue("country", reportArguments.getCountry());
        }

        return parameterSource;
    }
}
