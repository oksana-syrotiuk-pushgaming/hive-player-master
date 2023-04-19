package io.gsi.hive.platform.player.txn.report;

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
public class TxnReportDao {
	
	private static final Logger logger = LoggerFactory.getLogger(TxnReportDao.class);

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;



	public TxnReportDao(
			@Qualifier("reportNamedParameterJdbcTemplate")
			NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public List<TxnReportRecord> reportQuery(TxnReportArguments txnReportArguments)
	{
		TxnReportSqlBuilder sqlBuilder = TxnReportSqlBuilder.aTxnReportSqlBuilder(txnReportArguments);
		SqlParameterSource parameterSource = populateSqlParameterMap(txnReportArguments);

		String stakeSumQuery = sqlBuilder.buildStakeSumQuery();
		List<TxnReportRecord> recordsWithTotalStake = this.namedParameterJdbcTemplate.query(
				stakeSumQuery, parameterSource, new StakeSumQueryMapper(txnReportArguments));
		
		String winSumQuery = sqlBuilder.buildWinSumQuery();
		List<TxnReportRecord> recordsWithTotalWin = this.namedParameterJdbcTemplate.query(
				winSumQuery, parameterSource, new WinSumQueryMapper(txnReportArguments));
		
		String bonusInfoQuery = sqlBuilder.buildBonusInfoQuery();
		List<TxnReportRecord> recordsWithBonusInfo = this.namedParameterJdbcTemplate.query(
				bonusInfoQuery, parameterSource, new BonusInfoQueryMapper(txnReportArguments));
		
		String reportQuery = sqlBuilder.buildReportQuery();
		List<TxnReportRecord> reportQueryResults = this.namedParameterJdbcTemplate.query(
				reportQuery, parameterSource, new TxnReportRecordMapper(txnReportArguments));

		logger.info("Main Report Query: {} ", reportQuery);
		
		mergeTotalStakes(reportQueryResults,recordsWithTotalStake);
		mergeTotalWins(reportQueryResults,recordsWithTotalWin);
		mergeBonusInfo(reportQueryResults, recordsWithBonusInfo);

		return reportQueryResults;
	}

	private void mergeBonusInfo(List<TxnReportRecord> mainReportRecords, List<TxnReportRecord> recordsWithBonusInfo) {

		for (TxnReportRecord reportQueryResult : mainReportRecords)
		{
			for (TxnReportRecord bonus : recordsWithBonusInfo)
			{
				if (reportQueryResult.getIndex().equalsIgnoreCase(bonus.getIndex()))
				{
					reportQueryResult.setBonusCost(bonus.getBonusCost());
					reportQueryResult.setNumFreeRounds(bonus.getNumFreeRounds());
				}
			}
		}
	}

	private void mergeTotalStakes(List<TxnReportRecord> mainReportRecords, List<TxnReportRecord> totalStakeRecords) {
		for (TxnReportRecord reportQueryResult : mainReportRecords)
		{
			for (TxnReportRecord stake : totalStakeRecords)
			{
				if (reportQueryResult.getIndex().equalsIgnoreCase(stake.getIndex()))
				{
					reportQueryResult.setTotalStake(stake.getTotalStake());
				}
			}
		}
	}

	private void mergeTotalWins(List<TxnReportRecord> mainReportRecords, List<TxnReportRecord> totalWinRecords) {
		for (TxnReportRecord reportQueryResult : mainReportRecords)
		{
			for (TxnReportRecord stake : totalWinRecords)
			{
				if (reportQueryResult.getIndex().equalsIgnoreCase(stake.getIndex()))
				{
					reportQueryResult.setTotalWin(stake.getTotalWin());
				}
			}
		}
	}

	private SqlParameterSource populateSqlParameterMap(TxnReportArguments reportArguments)
	{
		MapSqlParameterSource parameterSource = new MapSqlParameterSource();

		Timestamp dateFrom = Timestamp.from(reportArguments.getDateFrom().toInstant());
		parameterSource.addValue("dateFrom", dateFrom, Types.TIMESTAMP);

		Timestamp dateTo = Timestamp.from(reportArguments.getDateTo().toInstant());
		parameterSource.addValue("dateTo", dateTo, Types.TIMESTAMP);

		parameterSource.addValue("igpCodes",reportArguments.getIgpCodes());
		parameterSource.addValue("mode",reportArguments.getMode().name());
		parameterSource.addValue("status",reportArguments.getStatus());

		if(reportArguments.getGuest() != null)
		{
			parameterSource.addValue("guest", reportArguments.getGuest());
		}

		if(reportArguments.getCcyCode() != null)
		{
			parameterSource.addValue("ccyCode", reportArguments.getCcyCode());
		}

		if(reportArguments.getCountry() != null)
		{
			parameterSource.addValue("country", reportArguments.getCountry());
		}

		if(reportArguments.getPlayerId() != null)
		{
			parameterSource.addValue("playerId", reportArguments.getPlayerId());
		}

		if(reportArguments.getUsername() != null)
		{
			parameterSource.addValue("username",reportArguments.getUsername());
		}

		if(reportArguments.getGameCode() != null)
		{
			parameterSource.addValue("gameCode", reportArguments.getGameCode());
		}

		if(reportArguments.getType() != null)
		{
			parameterSource.addValue("type", reportArguments.getType());
		}

		return parameterSource;
	}
}
