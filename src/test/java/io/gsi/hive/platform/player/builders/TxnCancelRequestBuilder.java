/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnCancelType;

public final class TxnCancelRequestBuilder {
	private Long timestamp;
	private String txnId;
	private String gameCode;
	private Boolean playComplete;
	private Boolean roundComplete;
	
	private TxnCancelType cancelType;

	public TxnCancelRequestBuilder() {
		this.timestamp = 1L;
		this.txnId = TxnPresets.TXNID;
		this.gameCode = GamePresets.CODE;
		this.playComplete = false;
		this.roundComplete = false;
		this.cancelType = TxnCancelType.RECON;
	}

	public TxnCancelRequestBuilder withTxnId(String txnId) {
		this.txnId = txnId;
		return this;
	}

	public TxnCancelRequestBuilder withGameCode(String gameCode) {
		this.gameCode = gameCode;
		return this;
	}

	public TxnCancelRequestBuilder withCancelType(TxnCancelType cancelType) {
		this.cancelType = cancelType;
		return this;
	}

	public TxnCancelRequestBuilder withPlayComplete(Boolean playComplete) {
		this.playComplete = playComplete;
		return this;
	}
	
	public TxnCancelRequestBuilder withRoundComplete(Boolean roundComplete) {
		this.roundComplete = roundComplete;
		return this;
	}

	public TxnCancelRequestBuilder withTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public TxnCancelRequest build() {
		TxnCancelRequest txnCancel = new TxnCancelRequest();
		txnCancel.setTxnId(txnId);
		txnCancel.setGameCode(gameCode);
		txnCancel.setCancelType(cancelType);
		txnCancel.setPlayComplete(playComplete);
		txnCancel.setRoundComplete(roundComplete);
		txnCancel.setTimestamp(timestamp);
		return txnCancel;
	}
	
	public static TxnCancelRequestBuilder txnCancelRequest()
	{
		return new TxnCancelRequestBuilder();
	}
}
