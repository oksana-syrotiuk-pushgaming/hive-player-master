package io.gsi.hive.platform.player.recon;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.WebAppException;
import io.gsi.commons.monitoring.MeterPublisher;
import io.gsi.hive.platform.player.exception.ApiException;
import io.gsi.hive.platform.player.exception.FreeroundsFundNotAvailableException;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.registry.ReconCounter;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.prometheus.client.Gauge;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReconTxnService {
	private static final Log logger = LogFactory.getLog(ReconTxnService.class);

	private final TxnRepository txnRepository;
	private final TxnService txnService;
	private final TxnCallbackRepository txnCallbackRepository;
	private final AutocompleteRequestRepository autocompleteRequestRepository;
	private final ReconCounter reconCounter;


	public ReconTxnService(TxnRepository txnRepository, TxnService txnService, TxnCallbackRepository txnCallbackRepository, AutocompleteRequestRepository autocompleteRequestRepository, ReconCounter reconCounter){
		this.txnRepository = txnRepository;
		this.txnService = txnService;
		this.txnCallbackRepository = txnCallbackRepository;
		this.autocompleteRequestRepository = autocompleteRequestRepository;
		this.reconCounter = reconCounter;
	}

	@Transactional(noRollbackFor={ApiException.class, WebAppException.class})
	public void reconcileTxnAndPlay(String txnId) {
		lockTransactionForRecon(txnId).ifPresent(txn -> {
			if (txn.isStake()) {
				reconcileStakeTxnAndPlay(txnId, txn);
			} else if (txn.isWin()) {
				reconcileWinTxnAndPlay(txnId, txn);
			} else {
				throw new InvalidStateException(String.format("transaction of type %s can't be recon", txn.getType()));
			}

			if (autocompleteRequestRepository.existsById(txn.getPlayId())) {
				autocompleteRequestRepository.deleteById(txn.getPlayId());
			}
		});
	}

	private void reconcileStakeTxnAndPlay(String txnId, Txn txn) {
		txnService.cancel(txn);
		txnCallbackRepository.saveToCallbackQueue(txnId, txn.getGameCode(), txn.getStatus().name());
	}


	private void reconcileWinTxnAndPlay(String txnId, Txn txn) {
		//Recon will now pick up txns with type cancelling, integrated from upstream.
		//Wins with Cancelling state should not really get this far, but better safe than sorry.
		if (txn.getStatus() == TxnStatus.CANCELLING) {
			throw new InvalidStateException(String.format("transaction of type %s can't be Cancelled", txn.getType()));
		} else {
			try {
				TxnReceipt txnReceipt = txnService.send(txn);
				txnCallbackRepository.saveToCallbackQueue(txnId, txn.getGameCode(), txnReceipt.getStatus().name());
			} catch (FreeroundsFundNotAvailableException ffe) {
				/*Freerounds expiry is (currently) the only situation in which txn will fail.
				 * We deal with the exception here, as recon has succeeded
				 */
				txnCallbackRepository.saveToCallbackQueue(txnId, txn.getGameCode(), TxnStatus.FAILED.name());
			}
		}
	}


	@Transactional
	public void incrementRetry(String txnId, Integer maxRetries, String exceptionName) {
		Txn txn = txnRepository.findAndLockByTxnId(txnId);
		txn.incrementRetry();
		if (txn.getRetry() >= maxRetries) {
			final var tags = Tags.of("Method","incrementRetry");
			reconCounter.increment(tags);
			txn.setStatus(TxnStatus.RECON);
			txn.setException(exceptionName);
		}
		txnRepository.save(txn);
	}

	/**
	 * Returns a batch of stake and win transactions that require reconciliation
	 * (state PENDING or CANCELLING)
	 *
	 * @param beforeTimestamp
	 * @param batchSize
	 * @return list of txn ids
	 */
	public List<String> getTxnsForRecon(ZonedDateTime beforeTimestamp, int batchSize) {

		return txnRepository.findReconTxns(beforeTimestamp, batchSize);
	}

	/**
	 * lock transaction for reconciliation
	 * @param txnId
	 * @return transaction or null if the transaction is no longer pending
	 */
	@Transactional(readOnly=false)
	public Optional<Txn> lockTransactionForRecon(String txnId) {
		Txn txn = txnRepository.findAndLockByTxnId(txnId);
		if(txn == null) {
			logger.warn(String.format("txnId=%s not found.",txnId));
			return Optional.empty();
		}
		if (txn.getStatus() != TxnStatus.PENDING && txn.getStatus() != TxnStatus.CANCELLING)  {
			logger.warn(String.format("txnId=%s already processed (%s)",txnId,txn.getStatus()));
			return Optional.empty();
		}
		if (!txn.isReconcilable()) {
			throw new InvalidStateException(
					String.format("txnId=%s, type=%s",txnId, txn.getType()));
		}
		return Optional.of(txn);
	}
}
