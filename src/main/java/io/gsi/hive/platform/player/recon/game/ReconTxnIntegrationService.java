package io.gsi.hive.platform.player.recon.game;

import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnRequest;

/**
 * This service takes a list of Game Txns pending in upstream games.
 * If the txns haven't reached us, they are integrated with regular recon.
 * If we have already processed a Txn but the game hasn't received it,
 * a callback is queued to update the game. 
 * */
@Service
public class ReconTxnIntegrationService {

	private static final Log logger = LogFactory.getLog(ReconTxnIntegrationService.class);

	private final TxnRepository txnRepository;
	private final TxnCallbackRepository txnCallbackRepository;
	private final TxnService txnService;

	public ReconTxnIntegrationService(TxnRepository txnRepository, TxnCallbackRepository txnCallbackRepository, TxnService txnService) {
		this.txnRepository = txnRepository;
		this.txnCallbackRepository = txnCallbackRepository;
		this.txnService = txnService;
	}

	/*Take a list of upstream pending txns
	 * If we haven't seen them, add them to recon,
	 * If we have but with state differences, update the game.
	 * Txns processed individually to take advantage of transactions
	 */
	@Transactional
	public void integrateGameTxn(TxnRequest gameTxn)
	{
		Txn stored = txnRepository.findAndLockByTxnId(gameTxn.getTxnId());
		//If the downstream txn isn't in the repo, it hasn't reached us - add it. Recon will pick it up
		if(stored == null)
		{
			logger.info("Integration: Previously unrecieved Txn: ["+gameTxn.getTxnId()+"] From game: " + gameTxn.getGameCode());
			txnService.create(gameTxn);
		}
		else //Txn is in the repo
		{
			/*If the txn was a stake and has gone thorough successfully, but
			 * Has not been communicated to the game, we assume the game has moved on and
			 * put it to recon for cancelling.
			 * 
			 * Later (In development, not this process) we may just change this to an OK callback, to allow games to recover.
			 * */
			if(stored.getStatus() == TxnStatus.OK && stored.isStake())
			{
				logger.info("Integration: Game "+gameTxn.getGameCode()+" did not recieve OK for Txn: ["+gameTxn.getTxnId()+"], Putting to recon");

				//We are having recon cancel this txn by resetting it to pending
				//TODO: this could now use cancelling status
				stored.setStatus(TxnStatus.PENDING);
				txnRepository.save(stored);
			}

			//Dont bother CB'ing a recon
			else if(stored.getStatus() == TxnStatus.RECON)
			{
				return;
			}

			//If the txn we have stored is not also pending, things have happened that have not reached the game - update them
			else if(stored.getStatus() != TxnStatus.PENDING)
			{
				//Add to txn callback queue repo
				if (!txnCallbackRepository.isInCallbackQueue(stored.getTxnId())) {
					logger.info("Integration: Adding callback to game: "+gameTxn.getGameCode()+" For unrecieved txn: [" + gameTxn.getTxnId()+"], Status: " + stored.getStatus().name());
					txnCallbackRepository.saveToCallbackQueue(gameTxn.getTxnId(), stored.getGameCode(), stored.getStatus().name());
				}
			}
		}
	}

	//TODO: what to do for pendings?
	@Transactional
	public void integrateCancelTxn(TxnCancelRequest cancelRequest)
	{
		Txn stored = txnRepository.findAndLockByTxnId(cancelRequest.getTxnId());

		/* We don't have the txn they're trying to cancel
		 * Technically a tombstone, but given we don't implement that here, 
		 * just callback a not found
		 * */
		if(stored == null)
		{
			logger.info("Integration: Game: " + cancelRequest.getGameCode() + ", Attempting to cancel NotFound txn: ["+cancelRequest.getTxnId()+"]");

			txnCallbackRepository.saveToCallbackQueue(cancelRequest.getTxnId(), cancelRequest.getGameCode(), TxnStatus.NOTFOUND.name());
		}		
		else//We have the txn
		{
			//No cancelling a Win
			if(stored.isWin())
			{
				return;
			}

			//We have Txn as ok, but they want to cancel, so their first request likely hasn't reached us.
			//Do the cancel.
			if(stored.getStatus() == TxnStatus.OK)
			{
				logger.info("Integration: Previously unrecieved Cancel for: ["+cancelRequest.getTxnId()+"] From game: " + cancelRequest.getGameCode());

				//We are having recon cancel this txn by resetting it to canceling
				stored.addEvent(cancelRequest);
				stored.setStatus(TxnStatus.CANCELLING);
				txnRepository.save(stored);
			}
			//Cancel is currently being processed (shouldn't really get here because of lock)
			else if(stored.getStatus() == TxnStatus.CANCELLING || stored.getStatus() == TxnStatus.RECON)
			{
				return;
			}
			/*Everything else, just call back with updated state
			 * includes FAILED, PENDING and CANCELLED
			 * TODO: PENDING is technically invalid for a cancel, but given this is a callback
			 * there's not much we can do other than update the game with the current state.
			 * CANCELLED just means that a previous reciept to the game hasn't made it, so a cb is fine
			 * */
			else
			{
				//If we're not waiting to callback already, add
				if(!txnCallbackRepository.isInCallbackQueue(cancelRequest.getTxnId()))
				{
					logger.info("Integration: Adding Callback to game: "+cancelRequest.getGameCode()+" For txn: [" + cancelRequest.getTxnId()+"], Status: " + stored.getStatus().name());
					txnCallbackRepository.saveToCallbackQueue(cancelRequest.getTxnId(), stored.getGameCode(), stored.getStatus().name());
				}
			}
		}
	}
}
