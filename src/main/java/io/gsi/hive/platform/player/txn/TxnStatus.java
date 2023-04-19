/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import java.util.Arrays;

/**
 * Enumeration of possible rhino txn statuses
 */
public enum TxnStatus {
	/**
	 * Txn has not yet been processed
	 */
	PENDING,
	/**
	 * Txn processing was successful
	 */
	OK,
	/**
	 * Txn processing definitively failed
	 */
	FAILED,
	/**
	 * Txn has been cancelled successfully
	 */
	CANCELLED,
	/**
	 * Txn is in a manual RECON state where Hive and iGP need to compare records before manually altering status.
	 */
	RECON,
	/**
	 * Txn is pending being cancelled
	 */
	CANCELLING,
	/**
	 * Descriptive failure state for txns not found - e.g. on a cancel
	 */
	NOTFOUND;

	public static TxnStatus findByName(String statusName)
	{
		return Arrays.stream(values()).filter(s -> s.name().equals(statusName))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("status does not match"));
	}
}
