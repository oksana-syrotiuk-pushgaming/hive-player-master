package io.gsi.hive.platform.player.txn.report;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class TxnReportSqlBuilder
{
    private static final String FROM_T_TXN = " FROM t_txn t ";

    private static final String AGGREGATE_SELECT_COLUMNS_FOR_REPORT_SQL = " count(*), " +
            "count(distinct t.player_id) as unique_players, count(distinct t.play_id) as num_plays, " +
            "sum(t.amount) as sum ";

    private static final String INNER_JOIN_WITH_T_PLAYER = " INNER JOIN t_player p "+
            "ON  t.player_id = p.player_id "+
            "AND t.igp_code=p.igp_code "+
            "AND t.guest=p.guest ";

    private static final String JOIN_CLAUSE_FOR_T_BIGWIN = " LEFT JOIN t_bigwin w ON t.ccy_code = w.ccy_code";
    
    private static final String WHERE_CLAUSE_FOR_IGP_CODE = " WHERE t.igp_code IN (:igpCodes) ";
    
    private Set<String> txnTablePrefixedColumns;
    private Set<String> playerTablePrefixedColumns;

    private TxnReportArguments reportArguments;
    private StringBuilder sqlQueryBuilder;

    private TxnReportSqlBuilder(TxnReportArguments reportArguments)
    {
        this.reportArguments = reportArguments;
        this.sqlQueryBuilder = new StringBuilder();
        this.playerTablePrefixedColumns = new HashSet<>();
        this.playerTablePrefixedColumns.add("country");
        this.playerTablePrefixedColumns.add("player_id");
        this.playerTablePrefixedColumns.add("username");
        this.txnTablePrefixedColumns = new HashSet<>();
        this.txnTablePrefixedColumns.add("ccy_code");
        this.txnTablePrefixedColumns.add("game_code");
    }

    public TxnReportSqlBuilder withNewStringBuilder()
    {
        this.sqlQueryBuilder = new StringBuilder();
        return this;
    }

    public static TxnReportSqlBuilder aTxnReportSqlBuilder(TxnReportArguments reportArguments)
    {
        return new TxnReportSqlBuilder(reportArguments);
    }

    private TxnReportSqlBuilder asReportQuery()
    {
        this.sqlQueryBuilder.append(AGGREGATE_SELECT_COLUMNS_FOR_REPORT_SQL);
        return this;
    }

    @SuppressWarnings("unused")//May be used eventually
    private TxnReportSqlBuilder asTotalSumAggregateQuery()
    {
        this.sqlQueryBuilder.append("SELECT COUNT(*) FROM ( distinct ");
        this.sqlQueryBuilder.append(
                StringUtils.join(this.reportArguments.getGroupBy(),","));
        this.sqlQueryBuilder.append(") as totalSum");
        return this;
    }

    private TxnReportSqlBuilder fromTxnTable()
    {
        this.sqlQueryBuilder.append(FROM_T_TXN);
        return this;
    }

    private TxnReportSqlBuilder withPlayerTableInnerJoin() {
        this.sqlQueryBuilder.append(INNER_JOIN_WITH_T_PLAYER);
        return this;
    }

    private TxnReportSqlBuilder withIgpCode()
    {
        this.sqlQueryBuilder.append(WHERE_CLAUSE_FOR_IGP_CODE);
        return this;
    }

    private TxnReportSqlBuilder withDateFrom()
    {
        if(reportArguments.getDateFrom() != null)
        {
            this.sqlQueryBuilder.append(" AND t.txn_ts >= :dateFrom");
        }
        return this;
    }

    private TxnReportSqlBuilder withDateTo()
    {
        if(reportArguments.getDateTo() != null)
        {
            this.sqlQueryBuilder.append(" AND t.txn_ts <= :dateTo");
        }
        return this;
    }

    private TxnReportSqlBuilder withPlayerId()
    {
        if(reportArguments.getPlayerId() != null)
        {
            this.sqlQueryBuilder.append(" AND t.player_id=:playerId");
        }
        return this;
    }

    private TxnReportSqlBuilder withUsername()
    {
        if(reportArguments.getUsername() != null)
        {
            this.sqlQueryBuilder.append(" AND p.username=:username");
        }
        return this;
    }

    private TxnReportSqlBuilder withGameCode()
    {
        if(reportArguments.getGameCode() != null)
        {
            this.sqlQueryBuilder.append(" AND t.game_code=:gameCode");
        }
        return this;
    }

    private TxnReportSqlBuilder withMode()
    {
        if(reportArguments.getMode() != null)
        {
            this.sqlQueryBuilder.append(" AND t.mode=:mode");
        }
        return this;
    }

    private TxnReportSqlBuilder withGuest()
    {
        this.sqlQueryBuilder.append(" AND t.guest=:guest");
        return this;
    }

    private TxnReportSqlBuilder withType()
    {
        if(reportArguments.getType() != null)
        {
            this.sqlQueryBuilder.append(" AND t.type=:type");
        }
        return this;
    }

    private TxnReportSqlBuilder forAllWinTxn()
    {
        this.sqlQueryBuilder.append(" AND t.type='WIN'");
        return this;
    }

    private TxnReportSqlBuilder forAllStakeTxn()
    {
        this.sqlQueryBuilder.append(" AND t.type='STAKE'");
        return this;
    }

    @SuppressWarnings("unused")//May be used eventually
	private TxnReportSqlBuilder forAllFreeRounds()
    {
        this.sqlQueryBuilder.append(" AND t.txn_events @@ 'fundId' ");
        return this;
    }

    private TxnReportSqlBuilder withStatus()
    {
        if(reportArguments.getStatus() != null)
        {
            this.sqlQueryBuilder.append(" AND t.status=:status");
        }
        return this;
    }

    private TxnReportSqlBuilder withCcyCode()
    {
        if(reportArguments.getCcyCode() != null)
        {
            this.sqlQueryBuilder.append(" AND t.ccy_code=:ccyCode");
        }
        return this;
    }

    private TxnReportSqlBuilder withCountry()
    {
        if(reportArguments.getCountry() != null)
        {
            this.sqlQueryBuilder.append(" AND p.country=:country");
        }
        return this;
    }

    private TxnReportSqlBuilder withBonus()
    {
        if(reportArguments.getBonus() != null)
        {
            this.sqlQueryBuilder.append(" AND t.txn_events @@ 'fundId'  ");
        }
        return this;
    }

    private TxnReportSqlBuilder withBigwinJoin()
    {
        if(reportArguments.getFilterBigWins())
        {
            this.sqlQueryBuilder.append(JOIN_CLAUSE_FOR_T_BIGWIN);
        }
        return this;
    }
    
    private TxnReportSqlBuilder withBigwinFilter()
    {
        if(reportArguments.getFilterBigWins())
        {
            this.sqlQueryBuilder.append(" AND (t.type = 'STAKE' OR (t.type = 'WIN' AND t.amount > COALESCE(w.min_win, 0)))");
        }
        return this;
    }
    
    
    private TxnReportSqlBuilder withOrderBy() {
        this.sqlQueryBuilder.append(" ORDER by t.igp_code , sum desc ");
        return this;
    }

    @SuppressWarnings("unused")//May be used eventually
    private TxnReportSqlBuilder withCountAll()
    {
        this.sqlQueryBuilder.append(" COUNT(*) as count_all ");
        return this;
    }

    private TxnReportSqlBuilder withSelectColumns()
    {
        Set<TxnGroupBy> columns = reportArguments.getGroupBy();
        if(!columns.isEmpty())
        {
            Set<String> selectColumns = new HashSet<>();
            columns.forEach(column ->
                    {
                        StringBuilder columnBuilder = new StringBuilder();
                        if (txnTablePrefixedColumns.contains(column.toString()))
                        {
                            columnBuilder.append("t.");
                        }
                        else if (playerTablePrefixedColumns.contains(column.toString()))
                        {
                            columnBuilder.append("p.");
                        }
                        columnBuilder.append(column.toString());
                        selectColumns.add(columnBuilder.toString());
                    }
            );
            this.sqlQueryBuilder.append("SELECT ");
            this.sqlQueryBuilder.append("t.igp_code, ");
            this.sqlQueryBuilder.append(
                    StringUtils.join(selectColumns,","));
        }
        return this;
    }

    private TxnReportSqlBuilder withGroupBy()
    {
        Set<TxnGroupBy> columns = reportArguments.getGroupBy();
        if(!columns.isEmpty())
        {
            Set<String> selectColumns = new HashSet<>();
            columns.forEach(column ->
                                {
                                    StringBuilder columnBuilder = new StringBuilder();
                                    if (txnTablePrefixedColumns.contains(column.toString()))
                                    {
                                        columnBuilder.append("t.");
                                    }
                                    else if (playerTablePrefixedColumns.contains(column.toString()))
                                    {
                                        columnBuilder.append("p.");
                                    }
                                    columnBuilder.append(column.toString());
                                    selectColumns.add(columnBuilder.toString());
                                }
            );
            this.sqlQueryBuilder.append(" GROUP BY ");
            this.sqlQueryBuilder.append("t.igp_code, ");
            this.sqlQueryBuilder.append(
                    StringUtils.join(selectColumns,","));
        }
        return this;
    }

    private TxnReportSqlBuilder withDelimiter()
    {
        this.sqlQueryBuilder.append(", ");
        return this;
    }

    private String getSqlString()
    {
        return sqlQueryBuilder.toString();
    }

    public String buildReportQuery()
    {
        return this.withNewStringBuilder()
                .withSelectColumns()
                .withDelimiter()
                .asReportQuery()
                .fromTxnTable()
                .withCommonQuery()
                .withGroupBy()
                .withOrderBy()
                .getSqlString();
    }

    public String buildStakeSumQuery()
    {
        this.withNewStringBuilder().withSelectColumns();
        this.sqlQueryBuilder.append(", sum(amount) as total_stake ");
        this.fromTxnTable().withCommonQuery().forAllStakeTxn().withGroupBy();
        return this.getSqlString();
    }

    public String buildWinSumQuery()
    {
        this.withNewStringBuilder().withSelectColumns();
        this.sqlQueryBuilder.append(", sum(amount) as total_win ");
        this.fromTxnTable().withCommonQuery().forAllWinTxn().withGroupBy();
        return this.getSqlString();
    }
    
    public String buildBonusInfoQuery()
    {
        this.withNewStringBuilder().withSelectColumns()
        .sqlQueryBuilder.append(", SUM(t.amount) as bonusCost, COUNT(*) AS numFreeRounds");
        this.fromTxnTable();
        this.sqlQueryBuilder.append("JOIN t_play ON t_play.play_id = t.play_id");
        this.withCommonQuery().forAllStakeTxn();
        this.sqlQueryBuilder.append(" AND t.status = 'OK' ");
        this.sqlQueryBuilder.append(" AND t_play.bonus_fund_type IS NOT NULL ");
        this.withGroupBy();
        return this.getSqlString();
    }

    private TxnReportSqlBuilder withCommonQuery() {
        return this.withPlayerTableInnerJoin()
        		.withBigwinJoin()
                .withIgpCode()
                .withDateFrom()
                .withDateTo()
                .withBonus()
                .withPlayerId()
                .withGuest()
                .withUsername()
                .withCcyCode()
                .withGameCode()
                .withMode()
                .withType()
                .withStatus()
                .withCcyCode()
                .withCountry()
                .withBigwinFilter();
    }
}
