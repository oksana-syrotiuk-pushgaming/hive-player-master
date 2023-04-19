package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.TxnCallback;
import io.gsi.hive.platform.player.txn.TxnStatus;

public  class TxnCallbackBuilder {

	private String txnId = TxnPresets.TXNID;
	private String gameCode = GamePresets.CODE;
	private TxnStatus status = TxnStatus.FAILED;
	private int retries= 0;
	private String exception = null;

	public static TxnCallbackBuilder txnCallback(){
		return new TxnCallbackBuilder();
	}
	
	public TxnCallbackBuilder withTxnId(String txnId) {
		this.txnId=txnId;
		return this;
	}
	public TxnCallbackBuilder withGameCode(String gameCode) {
		this.gameCode=gameCode;
		return this;
	}
	public TxnCallbackBuilder withStatus(TxnStatus status) {
		this.status=status;
		return this;
	}
	public TxnCallbackBuilder withRetries(int retries) {
		this.retries=retries;
		return this;
	}
	public TxnCallbackBuilder withException(String exception) {
		this.exception=exception;
		return this;
	}
	public TxnCallback build(){
		TxnCallback callback = new TxnCallback(txnId,gameCode,status);
		callback.setRetries(retries);
		callback.setException(exception);
		return callback;
	}
}
