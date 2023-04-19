package io.gsi.hive.platform.player.txn.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.persistence.TxnAuditRepository;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.TxnAudit;
import io.gsi.hive.platform.player.txn.TxnAuditAction;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL, PersistenceITBase.STAKE_TXN_SQL})
public class TxnAuditRepositoryIT extends PersistenceITBase {

	@Autowired
	private TxnAuditRepository txnAuditRepository;
	

	@Test 
	public void saveAndGetOk()
	{
		TxnAudit original = new TxnAudit(TxnPresets.TXNID, TxnAuditAction.FORCE_CANCELLED);
		txnAuditRepository.save(original);
		TxnAudit saved = txnAuditRepository.findById(TxnPresets.TXNID).get();
		assertThat(saved).isEqualTo(original);
	}	
}
