/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class TxnRepositoryImpl implements TxnRepositoryCustom {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Override
	public List<String> findReconTxns(ZonedDateTime before, int batchSize) {
		return this.jdbcTemplate.query(
			"select txn_id from t_txn where " + 
			" type in ('STAKE', 'WIN', 'OPFRSTK', 'OPFRWIN') " +
			" and txn_ts < '" + before.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "'" +
			" and status in ('PENDING', 'CANCELLING')" +
			" limit " + batchSize,
			new RowMapper<String>() {
				public String mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					return rs.getString("txn_id");
				}
			}
		);
	}

}
