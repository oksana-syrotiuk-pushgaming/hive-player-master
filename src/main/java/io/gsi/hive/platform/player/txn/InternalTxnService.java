package io.gsi.hive.platform.player.txn;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.demo.DemoWalletService;
import io.gsi.hive.platform.player.exception.TxnNotFoundException;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.mesh.mapping.MeshHiveMapping;
import io.gsi.hive.platform.player.persistence.TxnCleardownRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.event.HiveBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**TODO:
 * This service currently processes each type of request by first duplicating it, then creating a specific txn for it,
 * then passing that txn to the relevant Api service and recreating the request for the next application.
 *
 * It could likely be refactored to just send on the first duplicated request.
 * Each Txn should have a request and receipt pair, but the txns ids should conform to the application the request is for.
 *
 * */
@Service
@AllArgsConstructor
@Loggable
public class InternalTxnService {



	private final TxnRepository txnRepository;
	private final MeshService meshService;
	private final DemoWalletService demoWalletService;
	private final BonusWalletService bonusWalletService;
	private final SessionService sessionService;
	private final MeshHiveMapping meshHiveMapping;

	private final TxnCleardownRepository txnCleardownRepository;

	public TxnReceipt processDemoTxn(String rootTxnId) {

		Txn demoTxn = getTransaction(rootTxnId);

		TxnReceipt demoTxnReceipt = demoWalletService.sendTxn(demoTxn);

		return new TxnReceipt(demoTxnReceipt);
	}

	public TxnReceipt processRealTxn(String rootTxnId) {
		Txn meshTxn = getTransaction(rootTxnId);
		TxnReceipt meshTxnReceipt = meshService.sendTxn(meshTxn.getIgpCode(), meshTxn);
		return new TxnReceipt(meshTxnReceipt);
	}

	public TxnReceipt processOperatorFreeroundsTxn(Txn playerTxn, TxnRequest playerRequest) {
		TxnRequest meshRequest = meshHiveMapping.createOperatorFreeroundsTxnRequest(playerTxn);
		Txn meshTxn = getTransaction(meshRequest.getTxnId());

		OperatorBonusFundDetails bonusFundDetails =
				(OperatorBonusFundDetails) playerRequest.getBonusFundDetails();
		TxnReceipt meshTxnReceipt = meshService.sendOperatorFreeroundsTxn(
				meshTxn.getIgpCode(), meshTxn, bonusFundDetails);

		return new TxnReceipt(meshTxnReceipt);
	}

	//TODO: This can likely also be refactored to take txnId as opposed to txn
	public TxnReceipt processBonusTxn(Txn playerTxn) {
		Txn bonusTxn = getTransaction(playerTxn.getTxnId());

		return bonusWalletService.sendTxn(playerTxn.getIgpCode(), bonusTxn);
	}


	/**
	 * Send Bonus Winnings through mesh as RGS_FREEROUND_WIN and store the receipt
	 * This call is extra on top of the original txnRequest, so we have to artificially add another for auditing
	 * @param playerTxn
	 * @param finalFreeRoundsWin
	 * @param fund
	 * @return
	 */
	public TxnReceipt processFreeRoundsWin(Txn playerTxn, Boolean finalFreeRoundsWin, FreeroundsFund fund) {
		TxnRequest meshRequest = meshHiveMapping.createHiveFreeroundsWinTxnRequest(playerTxn);
		Txn meshTxn = getTransaction(meshRequest.getTxnId());

		return meshService.sendFreeroundsWinTxn(playerTxn.getIgpCode(), meshTxn, !finalFreeRoundsWin, fund);
	}

	/**
	 * Send Bonus Winnings through mesh on fund finishing RGS_FREEROUNDS_CLEARDOWN
	 * This call is extra on top of the original txnRequest, so we have to artificially add another for auditing
	 * close the bonus funds
	 * save the receipt, the txnCleardown, and add the clear down event to the txn
	 * @param playerTxn
	 * @param fund
	 * @return
	 */
	public TxnReceipt processFreeRoundsCleardown(Txn playerTxn, FreeroundsFund fund, String cleardownTxnId) {
		Long fundId = ((HiveBonusFundDetails) ((TxnRequest) playerTxn.getEvents()
				.stream()
				.filter(TxnRequest.class::isInstance)
				.findFirst()
				.get())
				.getBonusFundDetails()).getFundId();

		TxnRequest meshRequest = meshHiveMapping.createHiveFreeroundsCleardownTxnRequest(
				fund.getCumulativeWin(), cleardownTxnId, playerTxn);
		Txn meshTxn = createTransaction(playerTxn.getTxnId(), meshRequest);

		TxnReceipt meshTxnReceipt = meshService.sendFreeroundsCleardownTxn(fund.getCumulativeWin(),
				playerTxn.getIgpCode(), meshTxn, fund);

		//Close bonus on completion
		bonusWalletService.closeFund(fundId); //TODO: Does this need more finesse? error catching?

		var txnCleardown = new TxnCleardown();
		txnCleardown.setTxnId(playerTxn.getTxnId());
		txnCleardown.setTxnTs(ZonedDateTime.now(ZoneId.of("UTC")));
		txnCleardown.setCleardownTxnId(cleardownTxnId);
		txnCleardown.setAmount(meshRequest.getAmount());
		txnCleardownRepository.saveAndFlush(txnCleardown);

		return meshTxnReceipt;
	}

	private Txn getTransaction(String rootTxnId) {
		return txnRepository.findById(rootTxnId).orElseThrow(() -> new TxnNotFoundException("Txn not found"));
	}

	private Txn createTransaction(String rootTxnId, TxnRequest txnRequest) {
		Txn internalTxn = new Txn() {};

		Session session = sessionService.getSession(txnRequest.getSessionId());
		internalTxn.setAccessToken(session.getAccessToken());

		internalTxn.setTxnId(rootTxnId);

		internalTxn.setGameCode(txnRequest.getGameCode());
		internalTxn.setGuest(txnRequest.getGuest());
		internalTxn.setPlayId(txnRequest.getPlayId());
		internalTxn.setPlayComplete(txnRequest.getPlayComplete());
		internalTxn.setPlayCompleteIfCancelled(txnRequest.getPlayCompleteIfCancelled());
		internalTxn.setRoundId(txnRequest.getRoundId());
		internalTxn.setRoundComplete(txnRequest.getRoundComplete());
		internalTxn.setRoundCompleteIfCancelled(txnRequest.getRoundCompleteIfCancelled());
		internalTxn.setPlayerId(txnRequest.getPlayerId());
		internalTxn.setIgpCode(txnRequest.getIgpCode());
		internalTxn.setSessionId(txnRequest.getSessionId());
		internalTxn.setMode(txnRequest.getMode());
		internalTxn.setCcyCode(txnRequest.getCcyCode());
		internalTxn.setType(txnRequest.getTxnType());
		internalTxn.setAmount(txnRequest.getAmount());
		internalTxn.setJackpotAmount(txnRequest.getJackpotAmount());
		internalTxn.setStatus(TxnStatus.PENDING);
		internalTxn.setExtraInfo(txnRequest.getExtraInfo());

		internalTxn.addEvent(txnRequest);
		return internalTxn;
	}

	public void cancelBonusTxn(Txn playerTxn, TxnCancel txnCancel) {
		//If a stake txn has come from integration, it will not have an internal txn.
		//We need to create that here if need be.
		Txn bonusTxn = getTransaction(playerTxn.getTxnId());
		bonusWalletService.cancelTxn(playerTxn.getIgpCode(), bonusTxn, txnCancel);
	}

	public void cancelDemoTxn(Txn playerTxn, TxnCancel txnCancel) {
		//If a stake txn has come from integration, it will not have an internal txn.
		//We need to create that here if need be.
		Txn demoTxn = getTransaction(playerTxn.getTxnId());
		demoWalletService.cancelTxn(demoTxn, txnCancel);
	}

	public void cancelMeshTxn(Txn playerTxn, TxnCancel txnCancel) {
		//If a stake txn has come from integration, it will not have an internal txn.
		//We need to create that here if need be.
		Txn meshTxn = getTransaction(playerTxn.getTxnId());
		meshService.cancelTxn(playerTxn.getIgpCode(), meshTxn, txnCancel);
	}

	public void cancelOperatorFreeroundsTxn(Txn txn, TxnCancel txnCancel) {
		//If a stake txn has come from integration, it will not have an internal txn.
		//We need to create that here if need be.
		TxnRequest originalRequest = (TxnRequest) txn.getEvents().get(0);
		OperatorBonusFundDetails bonusFundDetails =
				(OperatorBonusFundDetails) originalRequest.getBonusFundDetails();
		Txn meshTxn = getTransaction(txn.getTxnId());
		meshService.cancelOperatorFreeroundsTxn(txn.getIgpCode(), meshTxn, txnCancel, bonusFundDetails);
	}
}
