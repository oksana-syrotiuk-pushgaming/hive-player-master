/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import java.util.Arrays;

public enum TxnType {
	STAKE,
	WIN,
	FRCLR,
	OPFRSTK,
	OPFRWIN,
	REFUND;

	public static TxnType findByName(String name) {
		return Arrays.stream(values())
				.filter(t -> t.name().equalsIgnoreCase(name))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("no match found for txn type"));
	}

	public TxnType toOperatorFreeroundsTxnType() {
		switch (this) {
			case STAKE:
				return TxnType.OPFRSTK;
			case WIN:
				return TxnType.OPFRWIN;
			default:
				throw new IllegalArgumentException("Unexpected TxnType " + this);
		}
	}
}
