/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.persistence;

import java.time.ZonedDateTime;
import java.util.List;

public interface TxnRepositoryCustom {

	List<String> findReconTxns(ZonedDateTime before, int batchSize);

}
