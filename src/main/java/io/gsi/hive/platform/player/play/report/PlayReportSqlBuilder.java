package io.gsi.hive.platform.player.play.report;

import io.gsi.hive.platform.player.txn.TxnType;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class PlayReportSqlBuilder {

  private static final String FROM_T_PLAY = " FROM t_play AS p";

  private static final String AGGREGATE_SELECT_COLUMNS_FOR_REPORT_SQL = " count(*), " +
      "count(distinct p.player_id) as unique_players, count(p.play_id) as num_plays, " +
      "sum(p.stake) as stake, sum(p.win) as win, (sum(p.stake) - sum(p.win)) as gross_gaming_revenue ";

  private static final String JOIN_CLAUSE_FOR_T_PLAYER =
      " INNER JOIN t_player AS a ON a.player_id = p.player_id " +
          "AND a.igp_code=p.igp_code " +
          "AND a.guest=p.guest ";

  private static final String WHERE_CLAUSE_FOR_IGP_CODE = " WHERE p.igp_code IN (:igpCodes) ";
  private static final String JOIN_CLAUSE_FOR_OPERATOR_FREEROUNDS = "LEFT OUTER JOIN t_txn AS t ON (t.play_id = p.play_id AND t.type = 'OPFRSTK')";

  private final PlayReportArguments reportArguments;
  private StringBuilder sqlQueryBuilder;

  private PlayReportSqlBuilder(PlayReportArguments reportArguments) {
    this.reportArguments = reportArguments;
    this.sqlQueryBuilder = new StringBuilder();
  }

  public PlayReportSqlBuilder withNewStringBuilder() {
    this.sqlQueryBuilder = new StringBuilder();
    return this;
  }

  public static PlayReportSqlBuilder aPlayReportSqlBuilder(PlayReportArguments reportArguments) {
    return new PlayReportSqlBuilder(reportArguments);
  }

  private PlayReportSqlBuilder asReportQuery() {
    this.sqlQueryBuilder.append(AGGREGATE_SELECT_COLUMNS_FOR_REPORT_SQL);
    return this;
  }

  private PlayReportSqlBuilder fromPlayTable() {
    this.sqlQueryBuilder.append(FROM_T_PLAY);
    return this;
  }

  private PlayReportSqlBuilder joinPlayerTable() {
    this.sqlQueryBuilder.append(JOIN_CLAUSE_FOR_T_PLAYER);
    return this;
  }

  private PlayReportSqlBuilder withIgpCode() {
    this.sqlQueryBuilder.append(WHERE_CLAUSE_FOR_IGP_CODE);
    return this;
  }

  private PlayReportSqlBuilder withDateFrom() {
    if (reportArguments.getDateFrom() != null) {
      this.sqlQueryBuilder.append(" AND p.modified_at >= :dateFrom");
    }
    return this;
  }

  private PlayReportSqlBuilder withDateTo() {
    if (reportArguments.getDateTo() != null) {
      this.sqlQueryBuilder.append(" AND p.modified_at <= :dateTo");
    }
    return this;
  }

  private PlayReportSqlBuilder withPlayerId() {
    if (reportArguments.getPlayerId() != null) {
      this.sqlQueryBuilder.append(" AND p.player_id=:playerId");
    }
    return this;
  }

  private PlayReportSqlBuilder withGameCode() {
    if (reportArguments.getGameCode() != null) {
      this.sqlQueryBuilder.append(" AND p.game_code=:gameCode");
    }
    return this;
  }

  private PlayReportSqlBuilder withMode() {
    if (reportArguments.getMode() != null) {
      this.sqlQueryBuilder.append(" AND p.mode=:mode");
    }
    return this;
  }

  private PlayReportSqlBuilder withGuest() {
    this.sqlQueryBuilder.append(" AND p.guest=:guest");
    return this;
  }

  private PlayReportSqlBuilder withStatus() {
    if (reportArguments.getStatus() != null) {
      this.sqlQueryBuilder.append(" AND p.status=:status");
    }
    return this;
  }

  private PlayReportSqlBuilder withCountry() {
    if (reportArguments.getCountry() != null) {
      this.sqlQueryBuilder.append(" AND a.country = :country");
    }
    return this;
  }

  private PlayReportSqlBuilder withCcyCode() {
    if (reportArguments.getCcyCode() != null) {
      this.sqlQueryBuilder.append(" AND p.ccy_code=:ccyCode");
    }
    return this;
  }

  private PlayReportSqlBuilder withFreeroundFilter() {
    if (reportArguments.getOnlyFreeroundPlays() == null) {
      return this;
    }

    if(Boolean.TRUE.equals(reportArguments.getOnlyFreeroundPlays())){
      this.sqlQueryBuilder.append(" AND (p.bonus_fund_type is not null OR t.type = 'OPFRSTK') ");
    } else {
      this.sqlQueryBuilder.append(" AND p.bonus_fund_type is null AND t.play_id is null ");
    }
    return this;
  }

  private PlayReportSqlBuilder withOrderBy() {
    this.sqlQueryBuilder.append(" ORDER BY p.igp_code ASC ,");
    if (!reportArguments.getOrderBy().isEmpty()) {
      this.sqlQueryBuilder.append(StringUtils.join(reportArguments.getOrderBy(), ",") + " desc ");
    } else {
      this.sqlQueryBuilder.append("stake desc ");
    }

    return this;
  }

  private PlayReportSqlBuilder withSelectColumns() {
    this.sqlQueryBuilder.append("SELECT ");
    this.sqlQueryBuilder.append("p.igp_code, ");
    this.sqlQueryBuilder.append(getGroupByColumns());
    this.sqlQueryBuilder.append(" ,");
    return this;
  }

  private PlayReportSqlBuilder withGroupBy() {
    this.sqlQueryBuilder.append(" GROUP BY ");
    this.sqlQueryBuilder.append("p.igp_code, ");
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
    return playGroupBy.equals(PlayGroupBy.country) ? "a." + playGroupBy : "p." + playGroupBy;
  }

  private String getSqlString() {
    return sqlQueryBuilder.toString();
  }

  public String buildReportQuery() {
    return this.withNewStringBuilder()
        .withSelectColumns()
        .asReportQuery()
        .fromPlayTable()
        .joinPlayerTable()
        .joinOperatorFreerounds()
        .withCommonQuery()
        .withFreeroundFilter()
        .withGroupBy()
        .withOrderBy()
        .getSqlString();
  }

  private PlayReportSqlBuilder joinOperatorFreerounds() {
    this.sqlQueryBuilder.append(JOIN_CLAUSE_FOR_OPERATOR_FREEROUNDS);
    return this;
  }

  private PlayReportSqlBuilder withCommonQuery() {
    return this.withIgpCode()
        .withDateFrom()
        .withDateTo()
        .withPlayerId()
        .withGuest()
        .withCcyCode()
        .withCountry()
        .withGameCode()
        .withMode()
        .withStatus()
        .withCcyCode();
  }
}
