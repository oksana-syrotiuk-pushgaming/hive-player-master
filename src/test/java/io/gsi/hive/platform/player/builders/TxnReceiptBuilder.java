package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.wallet.Wallet;

public class TxnReceiptBuilder {

	public static TxnReceiptBuilder txnReceipt(){
		return new TxnReceiptBuilder();
	}

	private String gameCode = GamePresets.CODE;
	private String txnId = TxnPresets.TXNID;
	private String txnRef = TxnPresets.TXNREF;
	private String playRef = TxnPresets.PLAYREF_NULL;
	private TxnStatus status = TxnStatus.OK;
	private Wallet wallet = WalletBuilder.aWallet().build();
	
	public TxnReceiptBuilder withGameCode(String gameCode) {
		this.gameCode=gameCode;
		return this;
	}
	public TxnReceiptBuilder withTxnId(String txnId) {
		this.txnId=txnId;
		return this;
	}
	public TxnReceiptBuilder withTxnRef(String txnRef) {
		this.txnRef=txnRef;
		return this;
	}

	public TxnReceiptBuilder withPlayRef(String playRef) {
		this.playRef=playRef;
		return this;
	}
	public TxnReceiptBuilder withStatus(TxnStatus status) {
		this.status=status;
		return this;
	}
	public TxnReceiptBuilder withWallet(Wallet wallet) {
		this.wallet = wallet;
		return this;
	}

	public TxnReceipt build(){
		TxnReceipt reciept = new TxnReceipt();
		reciept.setWallet(wallet);
		reciept.setGameCode(gameCode);
		reciept.setStatus(status);
		reciept.setTxnId(txnId);
		reciept.setTxnRef(txnRef);
		reciept.setPlayRef(playRef);
		
		return reciept;
	}
}

