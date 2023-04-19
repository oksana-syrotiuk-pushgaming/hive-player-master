/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn.persistence;

import static io.gsi.hive.platform.player.txn.BonusFundDetailsPresets.defaultHiveBonusFundDetails;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.gsi.hive.platform.player.txn.TxnType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnStatus;

public class PlayerTxnRepositoryIT extends PersistenceITBase {

	@Autowired
	private TxnRepository txnRepository;

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertStake() {
		Txn txn = createTxn();
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		txn = txnRepository.saveAndFlush(txn);
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn",
				"play_id = '1000-10' ")).isEqualTo(1);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void givenValidStake_whenFindByPlayIdAndTxnTypeStake_thenStakeTxnReturned() {
		Txn txn = createTxn();
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		txn = txnRepository.saveAndFlush(txn);
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn",
				"play_id = '1000-10' ")).isEqualTo(1);

		Txn actual = txnRepository.findByPlayIdAndTypeIn(txn.getPlayId(), List.of(TxnType.STAKE)).get(0);

		assertThat(actual).isEqualTo(txn);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void givenValidStake_whenFindByPlayIdAndTxnTypeWin_thenNoTxnReturned() {
		Txn txn = createTxn();
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		txn = txnRepository.saveAndFlush(txn);
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn",
				"play_id = '1000-10' ")).isEqualTo(1);

		List<Txn> actual = txnRepository.findByPlayIdAndTypeIn(txn.getPlayId(), List.of(TxnType.WIN));

		assertThat(actual).isEmpty();
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertStakeWithTxnRequestEvent() {
		Txn txn = TxnBuilder.txn()
				.withTxnEvents(List.of(
						defaultWinTxnRequestBuilder()
							.bonusFundDetails(defaultHiveBonusFundDetails().build())
							.build())
						).build();
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		
		Txn storedTxn = txnRepository.saveAndFlush(txn);
		assertThat(storedTxn).isEqualToComparingFieldByFieldRecursively(txn);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn",
				"play_id = '1000-10' ")).isEqualTo(1);
	}


	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL,STAKE_TXN_SQL})
	@Test
	public void insertDuplicate() {
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn",
				"txn_id = '1000-1' and status = 'PENDING'")).isEqualTo(1);
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.OK);
		txn = txnRepository.saveAndFlush(txn);
		assertThat(txn.getStatus()).isEqualTo(TxnStatus.OK);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn",
				"txn_id = '1000-1' and status = 'OK'")).isEqualTo(1);
		thrown.expect(DataIntegrityViolationException.class);
		jdbcTemplate.execute(STAKE_TXN_SQL);
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL,STAKE_TXN_SQL})
	@Test
	public void selectByPrimaryKey() {

		Txn txn = txnRepository.findById(TxnPresets.TXNID).get();
		Txn txn1 = createTxn();
		assertThat(txn).isNotNull();
		assertThat(txn.getTxnId()).isEqualTo(TxnPresets.TXNID);
		assertThat(txn).isEqualToIgnoringGivenFields(txn1, "events");
		assertThat(txn.getEvents().size()).isEqualTo(1);
	}


	private Txn createTxn() {
		return TxnBuilder.txn()
				.withSessionId("session")
				.withMode(Mode.real)
				.withCancelTs(null)
				.withTxnRef(null)
				.withException(null)
				.withBalance(null)
				.withExtraInfo(null)
				.build();
	}
}
