/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * TxnNotFoundException
 * 
 * Txn not found on IGP
 *
 */
public class TxnNotFoundException extends ApiKnownException {

	private static final long serialVersionUID = 1569712248330835308L;
	private static final String ERROR_CODE = "TxnNotFound";
	
	public TxnNotFoundException(String message) {
		super(ERROR_CODE, message);
	}

	public TxnNotFoundException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
