package io.gsi.hive.platform.player.persistence;

import io.gsi.hive.platform.player.txn.TxnCallback;
import java.util.List;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

//Note that txnId is NOT unique in this repo, it is the gameTxnId
public interface TxnCallbackRepository extends JpaRepository<TxnCallback, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	TxnCallback findAndLockByTxnId(String txnId);

	@Query(nativeQuery=true, value=saveToCallbackQueueQuery)
	@Modifying
	void saveToCallbackQueue(
			@Param("txnId") String txnId,
			@Param("gameCode") String gameCode,
			@Param("txnStatus") String status);

	String saveToCallbackQueueQuery =
			"insert into t_txn_callback_q (txn_id, game_code, txn_status, retries) " +
					"values (:txnId, :gameCode, :txnStatus, 0)";

	@Query(nativeQuery=true, value=isInCallbackQueueQuery)
	boolean isInCallbackQueue(@Param("txnId") String txnId);

	String isInCallbackQueueQuery =
			"SELECT CASE WHEN COUNT(*) >=1 THEN" +
					"    CAST(true AS BOOLEAN) ELSE" +
					"	 CAST(false AS BOOLEAN) " +
					"END " +
					"FROM t_txn_callback_q " +
					"WHERE txn_id = :txnId " ;

	@Query(nativeQuery=true, value=deleteFromCallbackQueueQuery)
	@Modifying
	void deleteFromCallbackQueue(
			@Param("txnId") String txnId);

	String deleteFromCallbackQueueQuery =
			"DELETE FROM t_txn_callback_q " +
					"WHERE txn_id = :txnId " ;

	@Query(nativeQuery=true, value=getQueuedCallbacksQuery)
	List<TxnCallback> getQueuedCallbacks(
			@Param("size") Integer size,
			@Param("retryLimit") Integer retryLimit);

	String getQueuedCallbacksQuery =
			"SELECT * FROM t_txn_callback_q WHERE retries <= :retryLimit LIMIT :size";

	@Modifying
	@Query(nativeQuery=true, value=incrementRetriesAndSetExceptionQuery)
	void incrementRetriesAndSetException(
			@Param("txnId") String txnId,
			@Param("exception") String exception);

	String incrementRetriesAndSetExceptionQuery = "UPDATE t_txn_callback_q SET retries = retries + 1 , exception = :exception "
			+ "WHERE txn_id = :txnId";
}
