/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn.event;

import io.gsi.hive.platform.player.event.EventType;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.wallet.Wallet;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TxnReceipt is returned by TxnService after a transaction has been successfully processed. The receipt includes
 * the new player balance after the txn is processed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TxnReceipt extends TxnEvent {

	{
		type = EventType.txnReceipt;
	}
	
	private String gameCode;
	private String txnId;
	private String txnRef;
	private String playRef;
	private TxnStatus status;
	private Wallet wallet;
	
	public TxnReceipt() {}
	
	/**Note wallet is shallow*/
	public TxnReceipt(TxnReceipt receipt) {
		this();
		this.gameCode = receipt.gameCode;
		this.status = TxnStatus.valueOf(receipt.status.name());
		this.txnId = receipt.txnId;
		this.txnRef = receipt.txnRef;
		this.playRef = receipt.playRef;
		this.wallet = new Wallet(receipt.wallet);
	}
}
