/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.persistence;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.LockModeType;

import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import io.gsi.hive.platform.player.txn.Txn;

public interface TxnRepository extends JpaRepository<Txn, String>, TxnRepositoryCustom {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Txn findAndLockByTxnId(String txnId);

  List<Txn> findByPlayIdAndTypeIn(String playId, List<TxnType> txnTypes);

  long countByStatusAndTxnTsBefore(TxnStatus status, ZonedDateTime txnTs);
}
