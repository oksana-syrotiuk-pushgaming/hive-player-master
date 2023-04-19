package io.gsi.hive.platform.player.report;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class ReportingConfig {

  static class HikariReportingPool extends HikariDataSource {
    private Integer statementTimeoutSeconds;

    public void setStatementTimeoutSeconds(Integer statementTimeoutSeconds) {
      this.statementTimeoutSeconds = statementTimeoutSeconds;
    }

    @Override
    public Connection getConnection() throws SQLException {
      var conn = super.getConnection();
      var sql = "set statement_timeout = " + statementTimeoutSeconds * 1000;
      conn.prepareStatement(sql).execute();
      return conn;
    }
  }

  @Bean("reportNamedParameterJdbcTemplate")
  public NamedParameterJdbcTemplate reportNamedParameterJdbcTemplate(
      @Value("${spring.datasource.url}") String url,
      @Value("${spring.datasource.username}") String username,
      @Value("${spring.datasource.password}") String password,
      @Value("${hive.reports.sql.maxConnPoolSize:1}") Integer maxConnPoolSize,
      @Value("${hive.reports.sql.statementTimeoutSeconds:20}") Integer statementTimeoutSeconds
      ){

    return constructNameTemplate(url, username, password, maxConnPoolSize, statementTimeoutSeconds);
  }

  @Bean("reportNamedParameterJdbcTemplateLongerDefaultTimeout")
  public NamedParameterJdbcTemplate reportNamedParameterJdbcTemplateLongerDefaultTimeout(
          @Value("${spring.datasource.url}") String url,
          @Value("${spring.datasource.username}") String username,
          @Value("${spring.datasource.password}") String password,
          @Value("${hive.reports.sql.maxConnPoolSize:1}") Integer maxConnPoolSize,
          @Value("${hive.reports.sql.statementTimeoutSeconds:1200}") Integer statementTimeoutSeconds
  ){

    return constructNameTemplate(url, username, password, maxConnPoolSize, statementTimeoutSeconds);
  }

  private NamedParameterJdbcTemplate constructNameTemplate(String url, String username, String password, Integer maxConnPoolSize, Integer statementTimeoutSeconds) {
    var dataSource = new HikariReportingPool();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setPoolName("HikariPool-Reports");
    dataSource.setStatementTimeoutSeconds(statementTimeoutSeconds);
    dataSource.setMaximumPoolSize(maxConnPoolSize);

    var jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.setQueryTimeout(statementTimeoutSeconds);
    return new NamedParameterJdbcTemplate(jdbcTemplate);
  }

}
