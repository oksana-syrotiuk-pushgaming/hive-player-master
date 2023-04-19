package io.gsi.hive.platform.player.recon;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnAuditRepository;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.PlayService;
import io.gsi.hive.platform.player.registry.RegistryGateway;
import io.gsi.hive.platform.player.registry.txn.IGPCodes;
import io.gsi.hive.platform.player.txn.*;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.wallet.Wallet;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManualReconService {

	private final TxnRepository txnRepository;
	private final TxnCallbackRepository callbackRepository;
	private final TxnAuditRepository txnAuditRepository;
	private final AutocompleteRequestRepository autocompleteRequestRepository;
	private final PlayService playService;
	private final RegistryGateway registryGateway;

	@Value("${hive.recon.maxRetries:10}")
	private Integer maxRetries;

	@Transactional
	public TxnStatus requeueReconTxn(String txnId) {
		return this.executeRequeue(txnId, 0);
	}

	@Transactional
	public TxnStatus requeueReconTxn(String txnId, Integer retry) {
		if(retry == null) {
			retry = maxRetries;
		}

		if(retry < 1) {
			throw new BadRequestException("Number of attempts to make cannot be lower than one");
		}

		if(retry > this.maxRetries) {
			throw new BadRequestException("Recon retry number cannot be greater than the configured max recon retries");
		}

		return this.executeRequeue(txnId, this.maxRetries - retry);

	}

	private TxnStatus executeRequeue(String txnId, int retry) {
		Txn reconTxn = txnRepository.findById(txnId).orElseThrow(
				() -> new InvalidStateException("Txn not found. ID: " + txnId)
		);

		if(reconTxn.getStatus() != TxnStatus.RECON) {
			throw new InvalidStateException("Txn not in a RECON state, actual: " + reconTxn.getStatus().toString());
		}


		reconTxn.setStatus(TxnStatus.PENDING);
		reconTxn.setRetry(retry);
		txnRepository.saveAndFlush(reconTxn);

		return reconTxn.getStatus();
	}

	@Transactional
	public TxnStatus updateTxnStatus(String txnId, TxnStatus newStatus) {

		var txn = this.validateNewStatus(txnId, newStatus);

		if(txn.isWin() && newStatus == TxnStatus.OK) {
			okWinTransaction(txn);
			setPlayToFinished(txn);
		} else if(txn.isStake() && newStatus == TxnStatus.CANCELLED) {
			cancelStakeTransaction(txn);
		} else {
			throw new InvalidStateException("Attempting to set invalid status for txn type: " + txn.getType().name());
		}
		//We only allow manual adjustment to states than can be safely called back
		//(Only txns in RECON, stakes -> CANCELLEd, wins->OK)
		//Adding to callback here saves a little time over waiting for the game to report as missing again
		callbackRepository.saveToCallbackQueue(txnId, txn.getGameCode(), newStatus.name());
		if (autocompleteRequestRepository.existsById(txn.getPlayId())) {
			autocompleteRequestRepository.deleteById(txn.getPlayId());
		}

		return txn.getStatus();
	}

	private Txn validateNewStatus(String txnId, TxnStatus newStatus) {
		Txn txn = txnRepository.findById(txnId).orElseThrow(
				() -> new InvalidStateException("Txn not found. ID: " + txnId)
		);
		TxnStatus oldStatus = txn.getStatus();

		if(oldStatus.equals(newStatus)) {
			throw new InvalidStateException("Txn already in state: " + txn.getStatus().toString());
		}
		if(!oldStatus.equals(TxnStatus.RECON)) {
			throw new InvalidStateException("Only txns in RECON state can be updated, status: " + txn.getStatus().toString());
		}
		if(isBonusTransaction(txn) && txn.isWin()) {
			throw new BadRequestException("Bonus txns state cannot be updated, status: " + txn.getStatus().toString());
		}

		var igpCodes = registryGateway.getConfigIgpCodes();
		if(igpCodes.getIgpCodesList().contains(txn.getIgpCode())) {
			throw new BadRequestException(
					String.format(
							"Txn %s belongs to IGP Code %s for which status forcing is disabled",
							txn.getTxnId(),
							txn.getIgpCode()
					)
			);
		}

		return txn;
	}

	private void okWinTransaction(Txn txn) {
		txn.setStatus(TxnStatus.OK);
		txnRepository.saveAndFlush(txn);

		var audit = new TxnAudit(txn.getTxnId(), TxnAuditAction.FORCE_OK);
		txnAuditRepository.save(audit);
	}

	private void cancelStakeTransaction(Txn txn) {
		txn.setStatus(TxnStatus.CANCELLED);
		txnRepository.saveAndFlush(txn);

		playService.cancelStake(txn);

		var audit = new TxnAudit(txn.getTxnId(), TxnAuditAction.FORCE_CANCELLED);
		txnAuditRepository.save(audit);
	}

	private void setPlayToFinished(Txn txn) {
		TxnReceipt txnReceipt = new TxnReceipt();
		txnReceipt.setGameCode(txn.getGameCode());
		txnReceipt.setTxnId(txn.getTxnId());
		txnReceipt.setTxnRef(txn.getTxnRef());
		txnReceipt.setStatus(txn.getStatus());
		// updateFromTxnReceipt only cares about the txnRef so the wallet here is not important.
		txnReceipt.setWallet(new Wallet());

		playService.updateFromTxnReceipt(txn, txnReceipt);
	}

	private boolean isBonusTransaction(Txn txn) {
		return txn.getEvents().stream()
				.filter(TxnRequest.class::isInstance)
				.map(TxnRequest.class::cast)
				.findFirst()
				.map(TxnRequest::getBonusFundDetails).isPresent();
	}
}


