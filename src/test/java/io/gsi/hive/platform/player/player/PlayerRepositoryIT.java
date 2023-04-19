/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import javax.validation.ConstraintViolationException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

@Sql(statements= {PersistenceITBase.CLEAN_DB_SQL}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PlayerRepositoryIT extends PersistenceITBase {

	@Autowired
	private PlayerRepository playerRepository;

	@Test
	public void insert() {
		Player player = new Player();
		player.setPlayerId("player1");
		player.setIgpCode("iguana");
		player.setGuest(false);
		player.setCcyCode("GBP");
		player.setUsername("test1");
		player.setAlias("test1");
		player.setLang("en");
		player = playerRepository.saveAndFlush(player);
		assertThat(player.getPlayerId(),is("player1"));
		assertThat(player.getIgpCode(),is("iguana"));
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_player",
				"player_id = 'player1' and igp_code = 'iguana'"),is(1));
	}

	@Test
	public void insertNullPlayerId() {
		Player player = new Player();
		player.setIgpCode("iguana");
		player.setGuest(false);
		player.setCcyCode(null);
		player.setUsername("test1");
		player.setAlias("test1");
		player.setLang("en");
		thrown.expect(ConstraintViolationException.class);
		playerRepository.saveAndFlush(player);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertDuplicate() {
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_player",
				"player_id = 'player1' and igp_code = 'iguana'"),is(1));
		Player player = new Player();
		player.setPlayerId("player1");
		player.setIgpCode("iguana");
		player.setGuest(false);
		player.setCcyCode("GBP");
		player.setUsername("test1");
		player.setAlias("test1");
		player.setLang("en");
		playerRepository.saveAndFlush(player);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_player",
				"player_id = 'player1' and igp_code = 'iguana'"),is(1));
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void checkPrimaryKey() {
		thrown.expect(DuplicateKeyException.class);
		jdbcTemplate.execute(PLAYER_SQL);
	}
	

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertSamePlayerIdDifferentIgpCode() {
		Player player = new Player();
		player.setPlayerId("player1");
		player.setIgpCode("testIGP");
		player.setGuest(false);
		player.setCcyCode("GBP");
		player.setUsername("test1");
		player.setAlias("test1");
		player.setLang("en");
		playerRepository.saveAndFlush(player);
		assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "t_player"),is(2));
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void findByPrimaryKey() {
		Player player = playerRepository.findById(new PlayerKey("player1","iguana", false)).get();
		assertThat(player,is(notNullValue()));
		assertThat(player.getPlayerId(),is("player1"));
		assertThat(player.getIgpCode(),is("iguana"));
		assertThat(playerRepository.findById(new PlayerKey("player2","iguana", false)).isEmpty(),is(true));
	}

	@Test
	public void okMaxLangAndCountry() {
		Player player = PlayerBuilder.aPlayer()
				.withCountry("1234")
				.withLang("qwer")
				.build();
		player = playerRepository.saveAndFlush(player);
		assertThat(player.getCountry(),is("1234"));
		assertThat(player.getLang(),is("qwer"));
	}

	@Test(expected = ConstraintViolationException.class)
	public void failInvalidCountry() {
		Player player = PlayerBuilder.aPlayer()
				.withCountry("1A1")
				.build();
		playerRepository.saveAndFlush(player);
	}

	@Test(expected = ConstraintViolationException.class)
	public void failInvalidLang() {
		Player player = PlayerBuilder.aPlayer()
				.withLang("e11n")
				.build();
		playerRepository.saveAndFlush(player);
	}

}
