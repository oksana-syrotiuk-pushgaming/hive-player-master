/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.commons.exception.WebAppException;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.demo.DemoWalletService;
import io.gsi.hive.platform.player.exception.*;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.platformidentifier.PlatformIdentifierService;
import io.gsi.hive.platform.player.play.PlayService;
import io.gsi.hive.platform.player.registry.ReconCounter;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.event.*;
import io.gsi.hive.platform.player.wallet.Wallet;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Tags;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.util.Lists.newArrayList;

@AllArgsConstructor
@Service
public class TxnService {

	private final TxnRepository txnRepository;
	private final Validator validator;
	private final SessionService sessionService;

	private final InternalTxnService internalTxnService;
	private final MeshService meshService;
	private final DemoWalletService demoWalletService;
	private final BonusWalletService bonusWalletService;
	private final PlayService playService;
	private final PlatformIdentifierService platformIdentifierService;
	private final ReconCounter reconCounter;


	private static final Log logger = LogFactory.getLog(TxnService.class);

	/**
	 * This method should be exposed through the REST api
	 *
	 * Reason for noRollBack on RunTimeException.class:
	 * No DB saving takes place until create(), after passing validation.
	 * */
	@Timed
	@Transactional(noRollbackFor= {RuntimeException.class})
	public TxnReceipt process(TxnRequest gameTxn) {

		String txnId = gameTxn.getTxnId();

		Optional<Txn> idempotentTxn = txnRepository.findById(txnId);
		//Enforce Idempotency
		if(idempotentTxn.isPresent())
		{
			return createIdempotentTxnResponse(idempotentTxn.get());
		}
		else
		{
			final var txn = create(gameTxn);
			return send(txn);
		}
	}

	/**
	 * Produces a playerReceipt using the details from a bonus and mesh txn receipt
	 * @param bonusReceipt the txn receipt from the bonus wallet
	 * @param meshReceipt the txn receipt from mesh
	 * @return A player receipt containing details from both
	 */
	private TxnReceipt mergeBonusAndMeshReceipt(TxnReceipt bonusReceipt, TxnReceipt meshReceipt) {
		var playerReceipt = new TxnReceipt(bonusReceipt);
		playerReceipt.getWallet().setBalance(meshReceipt.getWallet().getBalance());
		playerReceipt.getWallet().getFunds().addAll(meshReceipt.getWallet().getFunds());
		playerReceipt.setStatus(meshReceipt.getStatus());
		playerReceipt.setTxnRef(meshReceipt.getTxnRef());
		playerReceipt.setPlayRef(meshReceipt.getPlayRef());
		return playerReceipt;
	}

	/**
	 * This method was made separate from process() for use by recon.
	 * The process of sending was initially two phase, save > send
	 * This was refactored to a single method used by the REST api.
	 * However, when reconciling/resending a win txn, we want to be able to send
	 * it without calling save again.
	 * This shouldn't be exposed directly by the REST api, is public because it's also used internally for recon
	 * */
	@Timed
	@Transactional(noRollbackFor= {RuntimeException.class})
	public TxnReceipt send(Txn txn)
	{
		TxnRequest playerRequest = (TxnRequest) txn.getEvents().stream().filter(TxnRequest.class::isInstance).findFirst().get();
		TxnReceipt playerReceipt = null;
		try {
			switch(txn.getMode())
			{
			case real: {
				playerReceipt = sendRealTxn(txn, playerRequest);
				break;
			}
			case demo: {
				playerReceipt = internalTxnService.processDemoTxn(txn.getTxnId());
				break;
			}
			default:{throw new InvalidStateException("Unknown game mode: " + txn.getMode().toString());}
			}
		} catch(FreeroundsFundNotAvailableException ffe) {
			if (txn.isWin()) {
				playService.voidPlay(txn.getPlayId());
			}else{
				playService.cancelStake(txn);
			}
			txn.setStatus(TxnStatus.FAILED);
			txn.setException(ffe.getCode());
			txnRepository.saveAndFlush(txn);
			throw ffe;
		} catch (ApiException ex) {
			if (ex.isKnown()) {
				if (txn.isStake()) {
					txn.setStatus(TxnStatus.FAILED);
					playService.cancelStake(txn);
				} else {
					final var tags = Tags.of("Method","send","Error", "api_exception");
					reconCounter.increment(tags);
					txn.setStatus(TxnStatus.RECON);
				}
			}
			txn.setException(ex.getCode());
			txnRepository.saveAndFlush(txn);
			throw ex;
		} catch (WebAppException ex) {
			if (ex.isKnown()) {
				if (txn.isStake()) {
					txn.setStatus(TxnStatus.FAILED);
					playService.cancelStake(txn);
				} else {
					final var tags = Tags.of("Method","send","Error", "web_app_exception");
					reconCounter.increment(tags);
					txn.setStatus(TxnStatus.RECON);
				}
			}
			txn.setException(ex.getCode());
			txnRepository.saveAndFlush(txn);
			throw ex;
		}
		catch(RestClientException rae) {
			txn.setException(rae.getMostSpecificCause().getClass().getSimpleName());
			txnRepository.saveAndFlush(txn);
			throw rae;
		}

		txn.setTxnRef(playerReceipt.getTxnRef());
		txn.setPlayRef(playerReceipt.getPlayRef());
		txn.setStatus(playerReceipt.getStatus());
		txn.setBalance(playerReceipt.getWallet().getBalance());
		txn.addEvent(playerReceipt);

		txnRepository.saveAndFlush(txn);
		playService.updateFromTxnReceipt(txn, playerReceipt);
		return playerReceipt;
	}

	/**This should be exposed by REST api*/
	@Timed
	@Transactional(noRollbackFor={ApiException.class})
	public TxnReceipt externalCancel(TxnCancelRequest cancelRequest)
	{
		String txnId = cancelRequest.getTxnId();

		Txn txn = txnRepository.findAndLockByTxnId(txnId);

		if(txn == null)
		{
			throw new NotFoundException("Txn Not Found");
		}
		if(txn.isWin())
		{
			throw new ClawbackNotSupportedException("Wins cannot be cancelled");
		}

		//Only allow an external cancel on an OK txn
		if(newArrayList(TxnStatus.OK, TxnStatus.PENDING, TxnStatus.CANCELLING).contains(txn.getStatus())) {
			txn.setStatus(TxnStatus.CANCELLING);
			txn.setPlayComplete(cancelRequest.getPlayComplete());
			txn.setRoundComplete(cancelRequest.getRoundComplete());

			txn.addEvent(cancelRequest);
			txn = cancel(txn);

			TxnReceipt receipt = createTxnCancelReceipt(txn, cancelRequest.getTxnId());

			txn.addEvent(receipt);
			txnRepository.saveAndFlush(txn);

			return receipt;
		}
		else if(txn.getStatus() == TxnStatus.FAILED) {
			TxnReceipt receipt = createTxnCancelReceipt(txn, cancelRequest.getTxnId());
			receipt.setStatus(TxnStatus.CANCELLED);
			return receipt;
		} else if(txn.getStatus() == TxnStatus.CANCELLED) {
			return createTxnCancelReceipt(txn, cancelRequest.getTxnId());
		} else if (txn.getStatus() == TxnStatus.RECON) {
			throwFilteredException(txn.getException());
		}
		throw new InternalServerException("Txn not in cancellable state. Current State: " + txn.getStatus().name());
	}

	/**This should not be exposed directly by the REST Api
	 * It's used internally for Recon, or wrapped by external cancel*/
	@Timed
	@Transactional(noRollbackFor={ApiException.class, WebAppException.class})
	public Txn cancel(Txn txn) {
		TxnCancel txnCancel = new TxnCancel();
		txnCancel.setPlayComplete(txn.isPlayCompleteIfCancelled());
		txnCancel.setRoundComplete(txn.isRoundCompleteIfCancelled());
		try {
			switch(txn.getMode())
			{
			case real: {
				if (isTxnUsingHiveBonusFund(txn)) {
					internalTxnService.cancelBonusTxn(txn, txnCancel);
				} else if (isTxnUsingOperatorBonusFund(txn)) {
					internalTxnService.cancelOperatorFreeroundsTxn(txn, txnCancel);
				} else {
					internalTxnService.cancelMeshTxn(txn, txnCancel);
				}
				break;
			}
			case demo: {
				internalTxnService.cancelDemoTxn(txn, txnCancel);
				break;
			}
			default:{throw new InvalidStateException("Unknown game mode: " +txn.getMode().toString());}
			}
		} catch (TxnNotFoundException ex) {
			//if txn can't be found, then cancel has been successful
			logger.info(String.format("%s not found", txn.toString()));
			txn.setStatus(TxnStatus.NOTFOUND);
		} catch (ApiException ex) {
			if (ex.isKnown()) {
				//no point going any further with this with automatic reconciliation - set to RECON for
				//manual intervention
				var tags = Tags.of("Method","cancel","Error", "api_exception");
				reconCounter.increment(tags);
				txn.setStatus(TxnStatus.RECON);
			}
			txn.setException(ex.getCode());
			txnRepository.saveAndFlush(txn);
			throw ex;
		}	catch (WebAppException ex) {
			if (ex.isKnown()) {
				//no point going any further with this with automatic reconciliation - set to RECON for
				//manual intervention
				var tags = Tags.of("Method","cancel","Error", "web_app_exception");
				reconCounter.increment(tags);
				txn.setStatus(TxnStatus.RECON);
			}
			txn.setException(ex.getCode());
			txnRepository.saveAndFlush(txn);
			throw ex;
		}
		txn.cancel();
		txn = txnRepository.saveAndFlush(txn);
		playService.cancelStake(txn);
		return txn;
	}

	/**This shouldn't be exposed directly by the REST Api
	 * It's used internally for Recon, specifically if a Txn exists upstream but
	 * hasn't reached us, we have to manually save*/
	@Timed
	@Transactional(noRollbackFor= {RuntimeException.class})
	public Txn create(TxnRequest gameTxn) {
		Set<ConstraintViolation<TxnRequest>> violations = validator.validate(gameTxn);
		if (!violations.isEmpty()) {
			throw new IllegalStateException(new ConstraintViolationException(violations));
		}
		Txn txn = createTxn(gameTxn);
		txn = txnRepository.saveAndFlush(txn);

		playService.addTxn(txn);
		return txn;
	}

	private TxnReceipt sendRealTxn(Txn txn, TxnRequest playerRequest) {
		TxnReceipt playerReceipt = null;
		if (isTxnUsingHiveBonusFund(txn)) {
			var bonusReceipt = internalTxnService.processBonusTxn(txn);
			if (isWin(playerRequest)) {
				playerReceipt = this.sendBonusTxnWin(txn, playerRequest, bonusReceipt);
			} else {
				playerReceipt = bonusReceipt;
			}
		} else if (isTxnUsingOperatorBonusFund(txn)) {
			playerReceipt = internalTxnService.processOperatorFreeroundsTxn(txn, playerRequest);
		} else {
			playerReceipt = internalTxnService.processRealTxn(txn.getTxnId());
		}
		return playerReceipt;
	}

	private TxnReceipt sendBonusTxnWin(Txn txn, TxnRequest playerRequest, TxnReceipt bonusReceipt){
		var bonusFund = bonusReceipt.getWallet().getFunds().stream()
				.filter(FreeroundsFund.class::isInstance)
				.map(FreeroundsFund.class::cast)
				.findFirst()
				.get();
		var isCleardown = isLastBonusWin(playerRequest, bonusFund);
		TxnReceipt meshReceipt = internalTxnService.
				processFreeRoundsWin(txn, isCleardown, bonusFund);
		if(isCleardown) {
			Long fundId = ((HiveBonusFundDetails) ((TxnRequest) txn.getEvents()
					.stream()
					.filter(TxnRequest.class::isInstance)
					.findFirst()
					.get())
					.getBonusFundDetails()).getFundId();
			String cleardownTxnId = platformIdentifierService.createFreeRoundsCleardownId(fundId.toString());
			meshReceipt = internalTxnService.processFreeRoundsCleardown(txn, bonusFund, cleardownTxnId);
			var txnCleardownEvent = TxnCleardownEvent.builder()
					.txnId(txn.getTxnId())
					.cleardownTxnId(cleardownTxnId)
					.amount(bonusFund.getCumulativeWin())
					.build();
			txn.addEvent(txnCleardownEvent);
		}
		return mergeBonusAndMeshReceipt(bonusReceipt, meshReceipt);
	}

	private boolean isWin(TxnRequest playerRequest) {
		return playerRequest.getTxnType().equals(TxnType.WIN);
	}

	private boolean isLastBonusWin(TxnRequest playerRequest, FreeroundsFund fundAfterBonusTxn) {
		return playerRequest.getTxnType().equals(TxnType.WIN) && fundAfterBonusTxn.getRemaining() == 0;
	}

	/**
	 * Used for Idempotency if a Txn already exists*/
	private TxnReceipt createIdempotentTxnResponse(Txn txn)
	{
		switch(txn.getStatus())
		{
		case OK : {//If it was ok, just give back a regular receipt with a new wallet
			TxnReceipt receipt = new TxnReceipt();
			receipt.setTxnId(txn.getTxnId());
			receipt.setGameCode(txn.getGameCode());
			receipt.setStatus(txn.getStatus());
			receipt.setTxnRef(txn.getTxnRef());

			//Because returning the wallet at the time of Txn is pretty useless
			//(Despite its idempotent purity) we're returning an up to date one.
			Wallet wallet;
			switch(txn.getMode())
			{
			case real: {
				wallet = meshService.getWallet(txn.getIgpCode(), txn.getPlayerId(), txn.getGameCode(), txn.getAccessToken());
				if(isTxnUsingHiveBonusFund(txn)) {
					Wallet bonusWallet = bonusWalletService.getWallet(txn.getIgpCode(), txn.getPlayerId(), txn.getGameCode(), txn.getCcyCode());
					wallet.getFunds().addAll(bonusWallet.getFunds());
				}
				break;}
			case demo: {wallet = demoWalletService.getWallet(txn.getIgpCode(), txn.getPlayerId(), txn.getGameCode()); break;}
			default:{throw new InternalServerException("Unknown game mode: " +txn.getMode().toString());}
			}

			receipt.setWallet(wallet);

			return receipt;
		}
		case PENDING: {
			if(txn.isWin()){
				return this.send(txn);
			}
			throw new InternalServerException("Txn Pending");

		}
		case FAILED: {
			throwFilteredException(txn.getException());
		}
		case RECON : {
			if(StringUtils.isEmpty(txn.getException())){
				throw new InternalServerException("Txn Currently in Reconciliation");
			}
			throwFilteredException(txn.getException());
		}
		case CANCELLED: {
			throw new TxnTombstoneException("Txn Previously Cancelled");
		}
		default:
			throw new InternalServerException("Txn in Unknown state");
		}
	}

	private Txn createTxn(TxnRequest gameTxn) {
		Txn txn = new Txn();

		Session session = sessionService.getSession(gameTxn.getSessionId());
		txn.setAccessToken(session.getAccessToken());

		txn.setTxnId(gameTxn.getTxnId());
		txn.setGameCode(gameTxn.getGameCode());
		txn.setGuest(gameTxn.getGuest());
		txn.setPlayId(gameTxn.getPlayId());
		txn.setPlayComplete(gameTxn.getPlayComplete());
		txn.setPlayCompleteIfCancelled(gameTxn.getPlayCompleteIfCancelled());
		txn.setRoundId(gameTxn.getRoundId());
		txn.setRoundComplete(gameTxn.getRoundComplete());
		txn.setRoundCompleteIfCancelled(gameTxn.getRoundCompleteIfCancelled());
		txn.setPlayerId(gameTxn.getPlayerId());
		txn.setIgpCode(gameTxn.getIgpCode());
		txn.setSessionId(gameTxn.getSessionId());
		txn.setMode(gameTxn.getMode());
		txn.setCcyCode(gameTxn.getCcyCode());
		txn.setType(getTxnType(gameTxn));
		txn.setAmount(gameTxn.getAmount());
		txn.setJackpotAmount(gameTxn.getJackpotAmount());
		txn.setStatus(TxnStatus.PENDING);

		txn.addEvent(gameTxn);
		txn.setExtraInfo(gameTxn.getExtraInfo());
		return txn;
	}

	private TxnReceipt createTxnCancelReceipt(Txn txn, String txnId) {
		TxnReceipt receipt = new TxnReceipt();
		receipt.setGameCode(txn.getGameCode());
		receipt.setStatus(txn.getStatus());
		receipt.setTxnId(txnId);
		receipt.setTxnRef(txn.getTxnRef());
		receipt.setWallet(null);
		return receipt;
	}

	/**Translate an Exception from a failed Txn into
	 * a suitable Equivalent to return upstream*/
	private void throwFilteredException(String exceptionStr)
	{
		UpstreamGameSupportedException exception;
		try{
			exception = UpstreamGameSupportedException.valueOf(exceptionStr);
		}
		catch(IllegalArgumentException e){
			throw new InternalServerException("Txn Previously Failed");
		}

		switch(exception){
		case TxnNotFoundException:{throw new TxnNotFoundException("");}
		case TxnFailedException: {throw new TxnFailedException("");}
		case PlayerStatusException: {throw new PlayerStatusException("");}
		case PlayerLimitException:{throw new PlayerLimitException("");}
		case InsufficientFundsException:{throw new InsufficientFundsException("");}
		default:
			throw new InternalServerException("Txn Previously Failed");
		}
	}

	private static boolean isTxnUsingHiveBonusFund(Txn txn) {
		return isTxnUsingBonusFund(txn, HiveBonusFundDetails.class);
	}

	private static boolean isTxnUsingOperatorBonusFund(Txn txn) {
		return isTxnUsingBonusFund(txn, OperatorBonusFundDetails.class);
	}

	private static boolean isTxnUsingBonusFund(Txn txn, Class<? extends BonusFundDetails> bonusDetailsClass) {
		TxnRequest request = (TxnRequest) txn.getEvents()
				.stream()
				.filter(TxnRequest.class::isInstance)
				.findFirst()
				.orElseThrow(() -> new InvalidStateException("No request found in txn: "  + txn.getTxnId()));
		return bonusDetailsClass.isInstance(request.getBonusFundDetails());
	}

	private static boolean isTxnUsingBonusFund(TxnRequest txnRequest,
			Class<? extends BonusFundDetails> bonusDetailsClass) {
		return bonusDetailsClass.isInstance(txnRequest.getBonusFundDetails());
	}

	private static TxnType getTxnType(TxnRequest txnRequest) {
		TxnType txnType = txnRequest.getTxnType();
		return isTxnUsingBonusFund(txnRequest, OperatorBonusFundDetails.class)
				? txnType.toOperatorFreeroundsTxnType()
				: txnType;
	}
}
