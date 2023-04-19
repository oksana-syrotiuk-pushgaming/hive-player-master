package io.gsi.hive.platform.player.persistence;

import io.gsi.hive.platform.player.txn.TxnCleardown;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TxnCleardownRepository extends JpaRepository<TxnCleardown, String> {
}
