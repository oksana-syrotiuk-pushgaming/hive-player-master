/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * TxnCancelledException
 * 
 * Txn already cancelled
 *
 */
public class TxnCancelledException extends ApiKnownException {

	private static final long serialVersionUID = 1687268521542520719L;
	private static final String ERROR_CODE = "TxnCancelled";
	
	public TxnCancelledException(String message) {
		super(ERROR_CODE, message);
	}

	public TxnCancelledException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
