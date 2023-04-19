package io.gsi.hive.platform.player.recon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.persistence.TxnRepository;

public class TxnRepositoryReconIT extends PersistenceITBase {

	@Autowired
	private TxnRepository txnRepository;

	@Sql(statements={CLEAN_DB_SQL,PLAYER_SQL,STAKE_TXN_SQL})
	@Test
	public void reconTxns() {
		ZonedDateTime before = ZonedDateTime.parse("1970-01-02T00:00:00+00:00:00[UTC]");
		List<String> txnKeys = txnRepository.findReconTxns(before,10);
		assertThat(txnKeys.size(),is(1));
		ZonedDateTime after = ZonedDateTime.parse("1969-01-01T01:00:00+00:00:00[UTC]");
		List<String> txnKeys2 = txnRepository.findReconTxns(after,10);
		assertThat(txnKeys2.size(),is(0));
	}

	@Sql(statements={CLEAN_DB_SQL, PLAYER_SQL,
			"insert into t_txn values ('1000-1','testGame', '10','f','f','10','f','f', 'player1','iguana','testToken'," +
					"'session', 'real','f','GBP','STAKE','10.00','0.0000','2016-03-15T00:00:00+00:00',null," +
					"null,null,'CANCELLING',null,null,0);"
	})
	@Test
	public void reconTxnsWithCancelling() {
		ZonedDateTime before = ZonedDateTime.parse("2016-03-16T12:00:00+00:00:00[UTC]");
		List<String> txnKeys = txnRepository.findReconTxns(before,10);
		assertThat(txnKeys.size(),is(1));
		ZonedDateTime after = ZonedDateTime.parse("2016-03-14T12:00:00+00:00:00[UTC]");
		List<String> txnKeys2 = txnRepository.findReconTxns(after,10);
		assertThat(txnKeys2.size(),is(0));
	}
}
