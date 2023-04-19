package io.gsi.hive.platform.player.report;

import io.gsi.hive.platform.player.PersistenceITBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties={"hive.reports.sql.statementTimeoutSeconds=1"})
public class HikariReportingPoolIT extends PersistenceITBase {

  @Qualifier("reportNamedParameterJdbcTemplate")
  @Autowired
  NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  @Test(expected = DataAccessResourceFailureException.class)
  public void queryTimeOut() {
    var sql = "select pg_sleep(3);";
    namedParameterJdbcTemplate.query(sql, rs -> { });
  }

  @Test
  public void noQueryTimeOut() {
    var sql = "select 1";
    namedParameterJdbcTemplate.query(sql, rs -> { });
  }
}