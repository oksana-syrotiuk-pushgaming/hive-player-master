package io.gsi.hive.platform.player.play.report;

import io.gsi.hive.platform.player.ApiITBase;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

public class PlayReportDaoIT extends ApiITBase  {

    @Autowired
    private PlayReportDao playReportDao;

    @Test
    public void givenAutowiredPlayReportDao_thenCorrectDefaultLongSqlTimeout(){

        NamedParameterJdbcTemplate namedParameterJdbcTemplate =
                (NamedParameterJdbcTemplate) ReflectionTestUtils.getField(playReportDao, "namedParameterJdbcTemplate");

        int queryTimeout = namedParameterJdbcTemplate.getJdbcTemplate().getQueryTimeout();

        Assertions.assertThat(queryTimeout).isEqualTo(1200);

    }
}
