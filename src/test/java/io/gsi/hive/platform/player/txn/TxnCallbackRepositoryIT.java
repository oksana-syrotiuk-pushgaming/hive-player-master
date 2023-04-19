package io.gsi.hive.platform.player.txn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Mode;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

public class TxnCallbackRepositoryIT extends PersistenceITBase{

	@Autowired
	private TxnRepository txnRepository;
	@Autowired
	private TxnCallbackRepository txnCallbackRepository;

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertToAndDeleteFromCallbackQueue()
	{
		Txn txn = createTxn();
		txn = txnRepository.saveAndFlush(txn);

		String txnId = txn.getTxnId();
		assertThat(txn.getTxnId(),equalTo(TxnPresets.TXNID));

		txnCallbackRepository.saveToCallbackQueue(txnId, txn.getGameCode(), txn.getStatus().name());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn_callback_q",
				String.format("txn_id = '%s'",txnId)),is(1));

		txnCallbackRepository.deleteFromCallbackQueue(txnId);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn_callback_q",
				String.format("txn_id = '%s'",txnId)),is(0));
	}

	@Sql(statements= {CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void findNonExistant() {
		TxnCallback callback = txnCallbackRepository.findAndLockByTxnId(TxnPresets.TXNID);

		assertThat(callback, equalTo(null));
	}

	@Sql(statements= {CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertToCallbackQueueWithNullTxnId() {
		Txn txn = createTxn();
		txn = txnRepository.saveAndFlush(txn);
		thrown.expect(DataIntegrityViolationException.class);
		txnCallbackRepository.saveToCallbackQueue(null, txn.getGameCode(), txn.getStatus().name());
	}

	// TODO Callback Id decoupled from player txn id
	@Ignore("Callback Id decoupled from player txn id")
	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void insertToCallbackQueueWithIncorrectTxnId() {
		Txn txn = createTxn();
		txn = txnRepository.saveAndFlush(txn);
		String txnId = txn.getTxnId();
		thrown.expect(DataIntegrityViolationException.class);
		txnCallbackRepository.saveToCallbackQueue(txnId + 100, txn.getGameCode(), txn.getStatus().name());
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void saveAndGetQueuedCallbacks()
	{
		Txn txn = createTxn();
		Txn txn2 = createTxn();
		txn2.setTxnId("2");
		txn = txnRepository.saveAndFlush(txn);
		txn2 = txnRepository.saveAndFlush(txn2);

		txnCallbackRepository.saveToCallbackQueue(txn.getTxnId(), txn.getGameCode(), txn.getStatus().name());
		txnCallbackRepository.saveToCallbackQueue(txn2.getTxnId(), txn2.getGameCode(), txn2.getStatus().name());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn_callback_q",
				String.format("game_code = '%s'","testGame")),is(2));

		List<TxnCallback> txns = txnCallbackRepository.getQueuedCallbacks(10, 5);

		assertThat(txns.size(), is(2));
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void saveThenAddRetry()
	{
		Txn txn = createTxn();
		txn = txnRepository.saveAndFlush(txn);

		txnCallbackRepository.saveToCallbackQueue(txn.getTxnId(), txn.getGameCode(), txn.getStatus().name());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn_callback_q",
				String.format("txn_id = '%s'",txn.getTxnId())),is(1));

		txnCallbackRepository.incrementRetriesAndSetException(txn.getTxnId(),"exception");

		TxnCallback callback = txnCallbackRepository.getOne(txn.getTxnId());

		assertThat(callback.getRetries(), is(1));
		assertThat(callback.getException(), is("exception"));
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void saveThenReconNotReturnedAsQueued()
	{
		Txn txn = createTxn();
		txn = txnRepository.saveAndFlush(txn);

		txnCallbackRepository.saveToCallbackQueue(txn.getTxnId(), txn.getGameCode(), txn.getStatus().name());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn_callback_q",
				String.format("txn_id = '%s'",txn.getTxnId())),is(1));

		txnCallbackRepository.incrementRetriesAndSetException(txn.getTxnId(),"exception");
		txnCallbackRepository.incrementRetriesAndSetException(txn.getTxnId(),"exception");
		
		assertThat(txnCallbackRepository.getQueuedCallbacks(10, 1), is(Collections.emptyList()));
		
		TxnCallback callback = txnCallbackRepository.getOne(txn.getTxnId());

		assertThat(callback.getRetries(), is(2));
		assertThat(callback.getException(), is("exception"));
		assertThat(callback.getStatus(), is(TxnStatus.PENDING));
	}
	
	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL})
	@Test
	public void saveThenCheckExists()
	{
		Txn txn = createTxn();
		txn = txnRepository.saveAndFlush(txn);

		txnCallbackRepository.saveToCallbackQueue(txn.getTxnId(), txn.getGameCode(), txn.getStatus().name());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_txn_callback_q",
				String.format("txn_id = '%s'",txn.getTxnId())),is(1));

		assertThat(txnCallbackRepository.isInCallbackQueue(txn.getTxnId()), equalTo(true));
	}

	private Txn createTxn() {
		return TxnBuilder.txn()
				.withPlayComplete(false)
				.withRoundComplete(false)
				.withPlayerId("player1")
				.withIgpCode("iguana")
				.withAccessToken("testToken")
				.withSessionId("testSession")
				.withMode(Mode.real)
				.withGuest(false)
				.withCcyCode("GBP")
				.withType(TxnType.STAKE)
				.withAmount(new BigDecimal("10.00"))
				.withJackpotAmount(new BigDecimal("0.00"))
				.withStatus(TxnStatus.PENDING)
				.withTxnTs(ZonedDateTime.parse("2016-03-15T00:00:00+00:00[UTC]"))
				.build();
	}
}
