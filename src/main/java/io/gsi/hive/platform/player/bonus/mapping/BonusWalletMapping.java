package io.gsi.hive.platform.player.bonus.mapping;

import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.event.BonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnCancelType;
import io.gsi.hive.platform.player.txn.event.TxnRequest;

import java.util.Optional;

public class BonusWalletMapping {

    //BonusWallet expected the hive internal concatenated format
    public static TxnRequest txnToTxnRequest(Txn txn) {
        BonusFundDetails bonusDetails = txn.getEvents().stream()
                .filter(TxnRequest.class::isInstance)
                .map(TxnRequest.class::cast)
                .findFirst()
                .map(TxnRequest::getBonusFundDetails)
                .orElseThrow(() -> new IllegalArgumentException("Bonus fund details not present."));

        return TxnRequest.builder()
                .amount(txn.getAmount())
                .ccyCode(txn.getCcyCode())
                .gameCode(txn.getGameCode())
                .guest(txn.getGuest())
                .igpCode(txn.getIgpCode())
                .jackpotAmount(txn.getJackpotAmount())
                .mode(txn.getMode())
                .playComplete(txn.getPlayComplete())
                .playCompleteIfCancelled(txn.isPlayCompleteIfCancelled())
                .playerId(txn.getPlayerId())
                .playId(txn.getPlayId())
                .roundComplete(txn.getRoundComplete())
                .roundCompleteIfCancelled(txn.isRoundCompleteIfCancelled())
                .roundId(txn.getRoundId())
                .sessionId(txn.getSessionId())
                .txnId(txn.getTxnId())
                .txnType(txn.getType())
                .bonusFundDetails(bonusDetails)
                .extraInfo(txn.getExtraInfo())
                .build();
    }

    public static TxnCancelRequest txnToTxnCancelRequest(Txn txn, TxnCancel cancel) {
        TxnCancelRequest cancelRequest = new TxnCancelRequest();
		/*
		  Due to a bug in this code, a txnCancelRequest will be added to the txn we are searching in each time a cancel is attempted,
		  e.g. during recon when retries are necessary.
		  If there is actually an external request however, it will be the correct type, as the first type extracted will be propagated
		  down through the duplicates.

		  TODO: Recon scenarios for Bonus Txns have not been well tested
		  */
        Optional<TxnCancelRequest> playerRequest = txn.getEvents().stream()
                .filter(TxnCancelRequest.class::isInstance).map(TxnCancelRequest.class::cast)
                .findFirst();

        //If the cancel was started externally, use the type from request,
        //if not it must have been recon
        if (playerRequest.isPresent()) {
            cancelRequest.setCancelType(playerRequest.get().getCancelType());
        } else {
            cancelRequest.setCancelType(TxnCancelType.RECON);
        }

        cancelRequest.setGameCode(txn.getGameCode());
        cancelRequest.setPlayComplete(cancel.isPlayComplete());
        cancelRequest.setRoundComplete(cancel.isRoundComplete());
        cancelRequest.setTxnId(txn.getTxnId());

        return cancelRequest;
    }
}
