/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * TxnFailedException
 * 
 * Txn failed on IGP
 *
 */
public class TxnFailedException extends ApiKnownException {

	private static final long serialVersionUID = 7013977347928155744L;
	private static final String ERROR_CODE = "TxnFailed";
	
	public TxnFailedException(String message) {
		super(ERROR_CODE, message);
	}

	public TxnFailedException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}
}
