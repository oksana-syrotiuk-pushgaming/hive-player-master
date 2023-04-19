package io.gsi.hive.platform.player.bonus;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus;
import io.gsi.hive.platform.player.bonus.gateway.BonusWalletGateway;
import io.gsi.hive.platform.player.bonus.mapping.BonusWalletMapping;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.springframework.stereotype.Service;

@Service
public class BonusWalletService {
	private final BonusWalletGateway gateway;

	public BonusWalletService(BonusWalletGateway gateway) {
		this.gateway = gateway;
	}
	
	public Wallet getWallet(String igpCode, String playerId, String gameCode,
      String ccyCode) {
		return gateway.getWallet(playerId, igpCode, gameCode, ccyCode);
	}

	public FreeRoundsBonusPlayerAwardStatus getBonusAwardStatus(String igpCode, Long fundId) {
		return gateway.getBonusAwardStatus(igpCode, fundId);
	}

	
	public TxnReceipt sendTxn(String igpCode, Txn txn) {
		TxnReceipt receipt = gateway.processTxn(BonusWalletMapping.txnToTxnRequest(txn));	
		//We used to rely on 'fundless wallet' behaviour to communicate wins on expired Funds.
		//All responses form BonusWallet should now return funds though, and if not are valid manual recon scenarios
		if(receipt.getWallet().getFunds().isEmpty()) {
			throw new InvalidStateException("Received no funds from BonusWallet");
		}
		return receipt;
	}

	
	public void cancelTxn(String igpCode, Txn txn, TxnCancel txnCancel) {
		TxnCancelRequest cancelRequest = BonusWalletMapping.txnToTxnCancelRequest(txn, txnCancel);
		txn.addEvent(cancelRequest);
		
		gateway.cancelTxn(cancelRequest, txn.getTxnId());
	}
	
	public void closeFund(Long fundId) {
		gateway.closeFund(fundId);
	}
}


