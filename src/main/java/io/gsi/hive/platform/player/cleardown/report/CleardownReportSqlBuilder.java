package io.gsi.hive.platform.player.cleardown.report;

import io.gsi.hive.platform.player.play.report.PlayGroupBy;
import io.gsi.hive.platform.player.txn.TxnStatus;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Collectors;

public class CleardownReportSqlBuilder {

  private static final String FROM_T_CLEARDOWN = " FROM t_txn_cleardown tc JOIN t_txn AS t ON t.txn_id = tc.txn_id";

  private static final String SELECT_COLUMNS_FOR_REPORT =
          "count(distinct t.player_id) as unique_players, count(t.txn_id) as num_cleardowns, " +
                  "sum(tc.amount) as win ";

  private static final String JOIN_CLAUSE_FOR_T_PLAYER =
      " INNER JOIN t_player AS a ON a.player_id = t.player_id " +
          "AND a.igp_code=t.igp_code " +
          "AND a.guest=t.guest ";

  private static final String WHERE_CLAUSE_FOR_IGP_CODE = " WHERE t.igp_code IN (:igpCodes) ";

  private final CleardownReportArguments reportArguments;
  private StringBuilder sqlQueryBuilder;

  private CleardownReportSqlBuilder(
      CleardownReportArguments reportArguments) {
    this.reportArguments = reportArguments;
    this.sqlQueryBuilder = new StringBuilder();
  }

  public CleardownReportSqlBuilder withNewStringBuilder() {
    this.sqlQueryBuilder = new StringBuilder();
    return this;
  }

  public static CleardownReportSqlBuilder aPlayReportSqlBuilder(
      CleardownReportArguments reportArguments) {
    return new CleardownReportSqlBuilder(reportArguments);
  }

  private CleardownReportSqlBuilder asReportQuery() {
    this.sqlQueryBuilder.append(SELECT_COLUMNS_FOR_REPORT);
    return this;
  }

  private CleardownReportSqlBuilder fromCleardownTable() {
    this.sqlQueryBuilder.append(FROM_T_CLEARDOWN);
    return this;
  }

  private CleardownReportSqlBuilder joinPlayerTable() {
    this.sqlQueryBuilder.append(JOIN_CLAUSE_FOR_T_PLAYER);
    return this;
  }

  private CleardownReportSqlBuilder withIgpCode() {
    this.sqlQueryBuilder.append(WHERE_CLAUSE_FOR_IGP_CODE);
    return this;
  }

  private CleardownReportSqlBuilder withDateFrom() {
    if (reportArguments.getDateFrom() != null) {
      this.sqlQueryBuilder.append(" AND tc.txn_ts >= :dateFrom");
    }
    return this;
  }

  private CleardownReportSqlBuilder withDateTo() {
    if (reportArguments.getDateTo() != null) {
      this.sqlQueryBuilder.append(" AND tc.txn_ts <= :dateTo");
    }
    return this;
  }

  private CleardownReportSqlBuilder withPlayerId() {
    if (reportArguments.getPlayerId() != null) {
      this.sqlQueryBuilder.append(" AND t.player_id=:playerId");
    }
    return this;
  }

  private CleardownReportSqlBuilder withGameCode() {
    if (reportArguments.getGameCode() != null) {
      this.sqlQueryBuilder.append(" AND t.game_code=:gameCode");
    }
    return this;
  }

  private CleardownReportSqlBuilder withMode() {
    if (reportArguments.getMode() != null) {
      this.sqlQueryBuilder.append(" AND t.mode=:mode");
    }
    return this;
  }

  private CleardownReportSqlBuilder withGuest() {
    this.sqlQueryBuilder.append(" AND t.guest=:guest");
    return this;
  }

  private CleardownReportSqlBuilder withCountry() {
    if (reportArguments.getCountry() != null) {
      this.sqlQueryBuilder.append(" AND a.country = :country");
    }
    return this;
  }

  private CleardownReportSqlBuilder withCcyCode() {
    if (reportArguments.getCcyCode() != null) {
      this.sqlQueryBuilder.append(" AND t.ccy_code=:ccyCode");
    }
    return this;
  }

  private CleardownReportSqlBuilder withStatus() {
    this.sqlQueryBuilder.append(" AND t.status = '");
    this.sqlQueryBuilder.append(TxnStatus.OK);
    this.sqlQueryBuilder.append("' ");
    return this;
  }

  private CleardownReportSqlBuilder withOrderBy() {
    this.sqlQueryBuilder.append(" ORDER BY t.igp_code ASC ");
    if (!reportArguments.getOrderBy().isEmpty()) {
      this.sqlQueryBuilder.append(", ");
      this.sqlQueryBuilder.append(StringUtils.join(reportArguments.getOrderBy(), ","));
      this.sqlQueryBuilder.append(" desc ");
    }
    return this;
  }

  private CleardownReportSqlBuilder withSelectColumns() {
    this.sqlQueryBuilder.append("SELECT ");
    this.sqlQueryBuilder.append("t.igp_code, ");
    this.sqlQueryBuilder.append(getGroupByColumns());
    this.sqlQueryBuilder.append(", ");
    return this;
  }

  private CleardownReportSqlBuilder withGroupBy() {
    this.sqlQueryBuilder.append(" GROUP BY ");
    this.sqlQueryBuilder.append("t.igp_code, ");
    this.sqlQueryBuilder.append(getGroupByColumns());
    return this;
  }

  private String getGroupByColumns() {
    return reportArguments.getGroupBy()
        .stream()
        .map(this::getColumnName)
        .collect(Collectors.joining(","));
  }

  private String getColumnName(PlayGroupBy playGroupBy) {
    return playGroupBy.equals(PlayGroupBy.country) ? "a." + playGroupBy : "t." + playGroupBy;
  }

  private String getSqlString() {
    return sqlQueryBuilder.toString();
  }

  public String buildReportQuery() {
    return this.withNewStringBuilder()
            .withSelectColumns()
            .asReportQuery()
            .fromCleardownTable()
            .joinPlayerTable()
            .withFilters()
            .withGroupBy()
            .withOrderBy()
            .getSqlString();
  }

  private CleardownReportSqlBuilder withFilters() {
    return this.withIgpCode()
            .withDateFrom()
            .withDateTo()
            .withStatus()
            .withPlayerId()
            .withGuest()
            .withCcyCode()
            .withCountry()
            .withGameCode()
            .withMode();
  }
}
