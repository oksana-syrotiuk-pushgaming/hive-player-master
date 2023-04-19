package io.gsi.hive.platform.player.txn.search;

public class TxnSearchSqlBuilder {
    private static final String SELECT_COLUMNS_FOR_COUNT_SQL = "SELECT COUNT(*) as total";

    private static final String SELECT_COLUMNS_FOR_SEARCH_SQL = "SELECT t.txn_id, t.game_code, t.play_id" +
            ",t.play_complete,t.play_complete_if_cancelled,t.round_id,t.round_complete," +
            "t.round_complete_if_cancelled, t.player_id, p.username, p.country, t.igp_code, " +
            "t.session_id, t.mode, t.guest, t.ccy_code, t.type, t.amount, " +
            "t.txn_ts, t.cancel_ts, t.txn_ref, t.status, t.access_token, "
            + "(EXISTS (SELECT bonus_fund_type FROM t_play tp WHERE tp.bonus_fund_type is not null and t.play_id=tp.play_id and t.txn_ts >= :dateFrom and t.txn_ts <= :dateTo)) AS bonus";

    private static final String FROM_CLAUSE_WITH_PLAYER_JOIN_SQL = " FROM t_txn t " +
            "INNER JOIN t_player p ON t.player_id = p.player_id " +
            "AND t.igp_code=p.igp_code " +
            "AND t.guest=p.guest ";

    private final TxnSearchArguments searchArguments;
    private StringBuilder sqlQueryBuilder;

    private TxnSearchSqlBuilder(TxnSearchArguments searchArguments) {
        this.searchArguments = searchArguments;
        this.sqlQueryBuilder = new StringBuilder();
    }

    public static TxnSearchSqlBuilder aTxnSearchSqlBuilder(TxnSearchArguments searchArguments) {
        return new TxnSearchSqlBuilder(searchArguments);
    }

    private TxnSearchSqlBuilder asSearchQuery() {
        this.sqlQueryBuilder.append(SELECT_COLUMNS_FOR_SEARCH_SQL)
                .append(FROM_CLAUSE_WITH_PLAYER_JOIN_SQL);
        return this;
    }

    private TxnSearchSqlBuilder asCountQuery() {
        this.sqlQueryBuilder.append(SELECT_COLUMNS_FOR_COUNT_SQL)
                .append(FROM_CLAUSE_WITH_PLAYER_JOIN_SQL);
        return this;
    }

    private TxnSearchSqlBuilder withFreeroundFilter() {
        if (searchArguments.getOnlyFreeroundTxns() == null) {
            return this;
        }

        this.sqlQueryBuilder.append(" AND ");

        if (!searchArguments.getOnlyFreeroundTxns()) {
            this.sqlQueryBuilder.append("NOT ");
        }

        this.sqlQueryBuilder.append(" EXISTS (SELECT bonus_fund_type FROM t_play WHERE bonus_fund_type is not null and t.play_id=t_play.play_id)");

        return this;
    }

    private TxnSearchSqlBuilder withMandatoryIgpCode() {
        this.sqlQueryBuilder.append("WHERE t.igp_code IN (:igpCodes) ");
        return this;
    }

    private TxnSearchSqlBuilder withDateFrom() {
        if (searchArguments.getDateFrom() != null) {
            this.sqlQueryBuilder.append(" AND t.txn_ts >= :dateFrom");
        }
        return this;
    }

    private TxnSearchSqlBuilder withDateTo() {
        if (searchArguments.getDateTo() != null) {
            this.sqlQueryBuilder.append(" AND t.txn_ts <= :dateTo");
        }
        return this;
    }

    private TxnSearchSqlBuilder withPlayerId() {
        if (searchArguments.getPlayerId() != null) {
            this.sqlQueryBuilder.append(" AND t.player_id=:playerId");
        }
        return this;
    }

    private TxnSearchSqlBuilder withUsername() {
        if (searchArguments.getUsername() != null) {
            this.sqlQueryBuilder.append(" AND p.username=:username");
        }
        return this;
    }

    private TxnSearchSqlBuilder withGameCode() {
        if (searchArguments.getGameCode() != null) {
            this.sqlQueryBuilder.append(" AND t.game_code=:gameCode");
        }
        return this;
    }

    private TxnSearchSqlBuilder withMode() {
        if (searchArguments.getMode() != null) {
            this.sqlQueryBuilder.append(" AND t.mode=:mode");
        }
        return this;
    }

    private TxnSearchSqlBuilder withGuest() {
        this.sqlQueryBuilder.append(" AND t.guest=:guest");
        return this;
    }

    private TxnSearchSqlBuilder withTxnType() {
        if (searchArguments.getType() != null) {
            this.sqlQueryBuilder.append(" AND t.type=:type");
        }
        return this;
    }

    private TxnSearchSqlBuilder withStatus() {
        if (searchArguments.getStatus() != null) {
            this.sqlQueryBuilder.append(" AND t.status=:status");
        }
        return this;
    }

    private TxnSearchSqlBuilder withCcyCode() {
        if (searchArguments.getCcyCode() != null) {
            this.sqlQueryBuilder.append(" AND t.ccy_code=:ccyCode");
        }
        return this;
    }

    private TxnSearchSqlBuilder withCountry() {
        if (searchArguments.getCountry() != null) {
            this.sqlQueryBuilder.append(" AND p.country=:country");
        }
        return this;
    }

    private TxnSearchSqlBuilder withTxnId() {
        if (searchArguments.getTxnId() != null) {
            this.sqlQueryBuilder.append(" AND t.txn_id=:txnId");
        }
        return this;
    }

    private TxnSearchSqlBuilder withPlayId() {
        if (searchArguments.getPlayId() != null) {
            this.sqlQueryBuilder.append(" AND t.play_id=:playId");
        }
        return this;
    }

    private TxnSearchSqlBuilder withPlayRef() {
        if (searchArguments.getPlayRef() != null) {
            this.sqlQueryBuilder.append(" AND t.play_ref=:playRef");
        }
        return this;
    }

    private TxnSearchSqlBuilder withTxnRef() {
        if (searchArguments.getTxnRef() != null) {
            this.sqlQueryBuilder.append(" AND t.txn_ref=:txnRef");
        }
        return this;
    }

    private TxnSearchSqlBuilder withAccessToken() {
        if (searchArguments.getAccessToken() != null) {
            this.sqlQueryBuilder.append(" AND t.access_token=:accessToken");
        }
        return this;
    }

    private TxnSearchSqlBuilder withOrderBy() {
        this.sqlQueryBuilder.append(" ORDER BY t.igp_code, t.txn_ts ");
        return this;
    }

    private TxnSearchSqlBuilder withPaging() {
        Long pageSize = Long.valueOf(searchArguments.getPageSize());
        Long page = Long.valueOf(searchArguments.getPage());
        this.sqlQueryBuilder.append(" LIMIT ");
        this.sqlQueryBuilder.append((pageSize == 0) ? " ALL " : pageSize);
        this.sqlQueryBuilder.append(" OFFSET ").append(page * pageSize);
        return this;
    }

    private String getSqlString() {
        return sqlQueryBuilder.toString();
    }

    private TxnSearchSqlBuilder withNewStringBuilder() {
        this.sqlQueryBuilder = new StringBuilder();
        return this;
    }

    private TxnSearchSqlBuilder withAllFilters() {
        return this.withMandatoryIgpCode().withFreeroundFilter()
                .withDateFrom().withDateTo().withPlayerId().withTxnRef().withPlayRef()
                .withUsername().withGameCode().withMode().withGuest().withTxnType()
                .withStatus().withCcyCode().withCountry().withTxnId().withAccessToken()
                .withPlayId();
    }

    public String buildSearchQuery() {
        return this.withNewStringBuilder()
                .asSearchQuery()
                .withAllFilters()
                .withOrderBy()
                .withPaging()
                .getSqlString();
    }

    public String buildCountQuery() {
        return this.withNewStringBuilder()
                .asCountQuery()
                .withAllFilters()
                .getSqlString();
    }
}
