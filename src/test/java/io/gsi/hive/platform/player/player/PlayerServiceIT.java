/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;


@Sql(statements= {PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PlayerServiceIT extends ApiITBase {

	private static final String  PLAYER_ID = "123456";
	private static final String  IGP_CODE = "iguana";

	private JdbcTemplate jdbcTemplate;
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Autowired
	private PlayerService playerService;
		
	@Test
	public void createOk() {
		Player testPlayer = testPlayer();
		playerService.save(testPlayer);
		Player player = playerService.get(new PlayerKey(PLAYER_ID, IGP_CODE, false));
		assertThat(player, equalTo(testPlayer));
		assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "t_player"),is(2));
	}

	private Player testPlayer() {
		Player player = new Player();
		player.setAlias("alias");
		player.setCcyCode("GBP");
		player.setGuest(false);
		player.setIgpCode(IGP_CODE);
		player.setPlayerId(PLAYER_ID);
		player.setUsername("username");
		player.setLang("en");
		return player;
	}

}
