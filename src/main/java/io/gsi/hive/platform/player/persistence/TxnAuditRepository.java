/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.persistence;

import io.gsi.hive.platform.player.txn.TxnAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TxnAuditRepository extends JpaRepository<TxnAudit,String> {

}
