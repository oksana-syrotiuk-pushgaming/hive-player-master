package io.gsi.hive.platform.player.txn;

/**
 * Details of the request to cancel a transaction.
 */
public class TxnCancel {
	private boolean playComplete;
	private boolean roundComplete;
	
	public TxnCancel() {
	}
	
	public boolean isPlayComplete() {
		return playComplete;
	}

	/**
	 * @param playComplete is the play completed after this txn is cancelled
	 */
	public void setPlayComplete(boolean playComplete) {
		this.playComplete = playComplete;
	}
	
	public boolean isRoundComplete() {
		return roundComplete;
	}

	/**
	 *
	 * @param roundComplete is the round completed after this txn is cancelled
	 */
	public void setRoundComplete(boolean roundComplete) {
		this.roundComplete = roundComplete;
	}
	
	
}
