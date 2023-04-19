package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.txn.TxnCancel;

public class TxnCancelBuilder {

	private boolean playComplete = false;
	private boolean roundComplete = false;
	
	public TxnCancelBuilder withPlayComplete(boolean playComplete) {
		this.playComplete = playComplete;
		return this;
	}

	public TxnCancelBuilder withRoundComplete(boolean roundComplete) {
		this.roundComplete = roundComplete;
		return this;
	}

	public static TxnCancelBuilder txnCancel()
	{
		return new TxnCancelBuilder();
	}
	
	public TxnCancel build()
	{
		TxnCancel cancel = new TxnCancel();
		
		cancel.setPlayComplete(playComplete);
		cancel.setRoundComplete(roundComplete);
		
		return cancel;
	}
}
