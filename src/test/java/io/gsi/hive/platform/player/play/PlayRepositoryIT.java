package io.gsi.hive.platform.player.play;

import static org.assertj.core.api.Assertions.assertThat;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.autocompletion.AutocompleteRequest;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.game.Game;
import io.gsi.hive.platform.player.game.GameBuilder;
import io.gsi.hive.platform.player.game.GameStatus;
import io.gsi.hive.platform.player.mapper.MapperConfig;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.player.PlayerRepository;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.util.CalendarConverter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

public class PlayRepositoryIT extends PersistenceITBase {

	@Autowired
	private PlayRepository playRepository;

	@Autowired
	private TxnRepository repository;

	@Autowired
  	private JdbcTemplate template;

	@Autowired private PlayerRepository playerRepository;

	@Autowired
	private AutocompleteRequestRepository autocompleteRequestRepository;

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertIntoRepository() {
		playRepository.saveAndFlush(PlayBuilder.play().build());

		Play returned = playRepository.getOne(TxnPresets.PLAYID);

		assertThat(PlayBuilder.play().build()).isEqualTo(returned);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
				"player_id = 'player1' and igp_code = 'iguana'")).isEqualTo(1);

		playRepository.delete(returned);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void getActivePlaysByIgpAndCutoff() {

		playerRepository.save(PlayerBuilder.aPlayer().withIgpCode("other").build());
		Play shouldBeRetrieved = playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));//Should be picked up
		Play shouldBeRetrieved2 = playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play2").withStatus(PlayStatus.ACTIVE)
				.withCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")).minusHours(2)).build()));//Should be picked up
		Play shouldNotBeRetrived = playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play3").withStatus(PlayStatus.ACTIVE)
				.withCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")).minusHours(2)).build()));
		Game game = GameBuilder.aGame().withCode(shouldNotBeRetrived.getGameCode()).withServiceCode("1011").withStatus(GameStatus.active).build();
		playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play3").withIgpCode("other").build()));//wrong Igp
		playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play4").build()));//not active
		playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play5")
				.withStatus(PlayStatus.ACTIVE)
				.withCreatedAt(ZonedDateTime.now(ZoneId.of("UTC"))).build()));//Not Old EnoughTO
		Txn safetxn = TxnBuilder.txn().withPlayId(shouldBeRetrieved.getPlayId()).withTxnId("1000-11").withStatus(TxnStatus.OK).withGameCode(shouldBeRetrieved.getGameCode()).build();
		Txn safetxn2 = TxnBuilder.txn().withPlayId(shouldBeRetrieved2.getPlayId()).withTxnId("1000-12").withStatus(TxnStatus.OK).withGameCode(shouldBeRetrieved2.getGameCode()).build();
		Txn txn = TxnBuilder.txn().withPlayId("play3").withStatus(TxnStatus.RECON).withGameCode(shouldNotBeRetrived.getGameCode()).build();
		txn = repository.saveAndFlush(txn);
		safetxn = repository.saveAndFlush(safetxn);
		safetxn2 = repository.saveAndFlush(safetxn2);
		String sql = "SELECT count(*) FROM t_txn;";
		boolean exists = false;
		Integer count = template.queryForObject(sql,Integer.class);
		exists = count > 2;
		assertThat(exists).isEqualTo(true);
		List<Play> plays = playRepository.findAllByStatusAndIgpCodeAndCreatedAtBefore(
				PlayStatus.ACTIVE.toString(),
				IgpPresets.IGPCODE_IGUANA,
				CalendarConverter.convertToCalendar(ZonedDateTime.now(MapperConfig.UTC_ZONE).minusHours(1))
		);

		assertThat(plays.size()).isEqualTo(2);
		assertThat(plays).contains(shouldBeRetrieved2);
		assertThat(plays).contains(shouldBeRetrieved);
		assertThat(plays).doesNotContain(shouldNotBeRetrived);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void getActivePlaysByIgpOnly() {

		playerRepository.save(PlayerBuilder.aPlayer().withIgpCode("other").build());
		Play shouldBeRetrieved = playRepository.saveAndFlush(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build());//Should be picked up
		Play shouldBeRetrieved2 = playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play2").withStatus(PlayStatus.ACTIVE)
				.withCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")).minusHours(2)).build());//Should be picked up
		Play shouldNotBeRetrived = playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play3").withStatus(PlayStatus.ACTIVE)
				.withCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")).minusHours(2)).build());//RECON

		Game game = GameBuilder.aGame().withCode(shouldNotBeRetrived.getGameCode()).withServiceCode("1011").withStatus(GameStatus.active).build();
		playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play4").withIgpCode("other").build());//wrong Igp
		playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play5").build());//not active

		Txn safetxn = TxnBuilder.txn().withPlayId(shouldBeRetrieved.getPlayId()).withTxnId("1000-11").withStatus(TxnStatus.OK).withGameCode(shouldBeRetrieved.getGameCode()).build();
		Txn safetxn2 = TxnBuilder.txn().withPlayId(shouldBeRetrieved2.getPlayId()).withTxnId("1000-12").withStatus(TxnStatus.OK).withGameCode(shouldBeRetrieved2.getGameCode()).build();
		Txn txn = TxnBuilder.txn().withPlayId(shouldNotBeRetrived.getPlayId()).withStatus(TxnStatus.RECON).withGameCode(shouldNotBeRetrived.getGameCode()).build();
		txn = repository.saveAndFlush(txn);
		safetxn = repository.saveAndFlush(safetxn);
		safetxn2 = repository.saveAndFlush(safetxn2);

    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate,"t_txn"));
		List<Play> plays = playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
				PlayStatus.ACTIVE.toString(),
				IgpPresets.IGPCODE_IGUANA);
		assertThat(plays.size()).isEqualTo(2);
		assertThat(plays).contains(shouldBeRetrieved2);
		assertThat(plays).contains(shouldBeRetrieved);
		assertThat(plays).doesNotContain(shouldNotBeRetrived);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void getActivePlaysExcludesAlreadyQueuedPlays() {

		playerRepository.save(PlayerBuilder.aPlayer().withIgpCode("other").build());
		Game game = GameBuilder.aGame().withCode("testGame").withServiceCode("1011").withStatus(GameStatus.active).build();
		Play shouldBeRetrieved = playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));//Should be picked up
		Play shouldBeExcluded = playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play2").withStatus(PlayStatus.ACTIVE)
				.withCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")).minusHours(2)).build()));//Should be excluded as already queued
				Txn safetxn = TxnBuilder.txn().withPlayId(shouldBeRetrieved.getPlayId()).withTxnId("1000-11").withStatus(TxnStatus.OK).withGameCode(shouldBeRetrieved.getGameCode()).build();
				Txn safetxn2 = TxnBuilder.txn().withPlayId(shouldBeExcluded.getPlayId()).withTxnId("1000-12").withStatus(TxnStatus.OK).withGameCode(shouldBeExcluded.getGameCode()).build();
				safetxn = repository.saveAndFlush(safetxn);
				safetxn2 = repository.saveAndFlush(safetxn2);
		playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play3").withIgpCode("other").build()));//wrong Igp
		playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play4").build()));//not active
		playRepository.saveAndFlush(playRepository.saveAndFlush(PlayBuilder.play().withPlayId("play5")
				.withStatus(PlayStatus.ACTIVE)
				.withCreatedAt(ZonedDateTime.now(ZoneId.of("UTC"))).build()));//Not Old Enough

		autocompleteRequestRepository.saveAndFlush(
				new AutocompleteRequest("play2", GamePresets.CODE, SessionPresets.SESSIONID,false)
		);

		List<Play> plays = playRepository.findAllByStatusAndIgpCodeAndCreatedAtBefore(
				PlayStatus.ACTIVE.toString(),
				IgpPresets.IGPCODE_IGUANA,
				CalendarConverter.convertToCalendar(ZonedDateTime.now(MapperConfig.UTC_ZONE).minusHours(1))
		);

		assertThat(plays.size()).isEqualTo(1);
		assertThat(plays.get(0)).isEqualTo(shouldBeRetrieved);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void saveAndLoadPlayRef() {
		final var play = PlayBuilder.play().withPlayRef("aRef").build();
		playRepository.saveAndFlush(play);

		final var returned = playRepository.getOne(TxnPresets.PLAYID);

		assertThat(returned).isEqualTo(play);
	}
}
