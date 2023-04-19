package io.gsi.hive.platform.player.play.search;


import org.apache.commons.lang3.StringUtils;

public class PlaySearchSqlBuilder {

	private static final String SELECT_COLUMNS_FOR_COUNT_SQL = "SELECT COUNT(*) as total";

	private static final String SELECT_COLUMNS_FOR_SEARCH_SQL = "SELECT p.play_id, p.status, p.created_at, p.modified_at," +
			" p.stake, p.win," +
			" p.player_id, p.igp_code," +
			" p.mode, p.guest, p.ccy_code," +
			" p.status, p.num_txns, p.game_code, " +
			" player.country, " +
			" (p.bonus_fund_type is not null) as is_free_round," +
			" p.play_ref";

	private static final String FROM_CLAUSE_WITH_PLAYER_JOIN_SQL = " FROM t_play p " +
			" INNER JOIN t_player player" +
			" ON p.player_id = player.player_id" +
			" AND p.igp_code=player.igp_code" +
			" AND p.guest=player.guest ";

	private PlaySearchArguments searchArguments;
	private StringBuilder sqlQueryBuilder;

	private PlaySearchSqlBuilder(PlaySearchArguments searchArguments) {
		this.searchArguments = searchArguments;
		this.sqlQueryBuilder = new StringBuilder();
	}

	public static PlaySearchSqlBuilder aPlaySearchSqlBuilder(PlaySearchArguments searchArguments) {
		return new PlaySearchSqlBuilder(searchArguments);
	}

	private PlaySearchSqlBuilder asSearchQuery() {
		this.sqlQueryBuilder.append(SELECT_COLUMNS_FOR_SEARCH_SQL)
				.append(FROM_CLAUSE_WITH_PLAYER_JOIN_SQL);
		return this;
	}

	private PlaySearchSqlBuilder asCountQuery() {
		this.sqlQueryBuilder.append(SELECT_COLUMNS_FOR_COUNT_SQL)
				.append(FROM_CLAUSE_WITH_PLAYER_JOIN_SQL);
		return this;
	}


	private PlaySearchSqlBuilder withMandatoryIgpCode() {
		this.sqlQueryBuilder.append("WHERE p.igp_code IN (:igpCodes) ");
		return this;
	}

	private PlaySearchSqlBuilder withDateFrom() {
		if (searchArguments.getDateFrom() != null) {
			this.sqlQueryBuilder.append(" AND p.modified_at >= :dateFrom");
		}
		return this;
	}

	private PlaySearchSqlBuilder withDateTo() {
		if (searchArguments.getDateTo() != null) {
			this.sqlQueryBuilder.append(" AND p.modified_at <= :dateTo");
		}
		return this;
	}

	private PlaySearchSqlBuilder withPlayerId() {
		if (searchArguments.getPlayerId() != null) {
			this.sqlQueryBuilder.append(" AND p.player_id=:playerId");
		}
		return this;
	}

	private PlaySearchSqlBuilder withGameCode() {
		if (StringUtils.isNotBlank(searchArguments.getGameCode())) {
			this.sqlQueryBuilder.append(" AND p.game_code=:gameCode");
		}
		return this;
	}

	private PlaySearchSqlBuilder withMode() {
		if (searchArguments.getMode() != null) {
			this.sqlQueryBuilder.append(" AND p.mode=:mode");
		}
		return this;
	}

	private PlaySearchSqlBuilder withGuest() {
		if (searchArguments.getGuest() != null) {
			this.sqlQueryBuilder.append(" AND p.guest=:guest");
		}
		return this;
	}


	private PlaySearchSqlBuilder withStatus() {
		if (searchArguments.getStatus() != null) {
			this.sqlQueryBuilder.append(" AND p.status=:status");
		}
		return this;
	}

	private PlaySearchSqlBuilder withCcyCode() {
		if (StringUtils.isNotBlank(searchArguments.getCcyCode())) {
			this.sqlQueryBuilder.append(" AND p.ccy_code=:ccyCode");
		}
		return this;
	}

	private PlaySearchSqlBuilder withPlayId() {
		if (StringUtils.isNotBlank(searchArguments.getPlayId())) {
			this.sqlQueryBuilder.append(" AND p.play_id=:playId");
		}
		return this;
	}

	private PlaySearchSqlBuilder withPlayRef() {
		if (searchArguments.getPlayRef() != null) {
			this.sqlQueryBuilder.append(" AND p.play_ref=:playRef");
		}
		return this;
	}

	private PlaySearchSqlBuilder withOrderBy() {
		this.sqlQueryBuilder.append(" ORDER BY p.igp_code, p.modified_at ");
		return this;
	}

	private PlaySearchSqlBuilder withPaging() {
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

	private PlaySearchSqlBuilder withNewStringBuilder() {
		this.sqlQueryBuilder = new StringBuilder();
		return this;
	}

	public String buildSearchQuery() {
		return this.withNewStringBuilder()
				.asSearchQuery().withMandatoryIgpCode().withDateFrom().withDateTo().withPlayerId()
				.withGameCode().withMode().withGuest().withPlayRef()
				.withStatus().withCcyCode()
				.withPlayId().withOrderBy().withPaging().getSqlString();
	}

	public String buildCountQuery() {
		return this.withNewStringBuilder()
				.asCountQuery().withMandatoryIgpCode().withDateFrom().withDateTo()
				.withPlayerId().withGameCode()
				.withMode().withGuest().withStatus()
				.withCcyCode()
				.withPlayId().getSqlString();
	}
}
