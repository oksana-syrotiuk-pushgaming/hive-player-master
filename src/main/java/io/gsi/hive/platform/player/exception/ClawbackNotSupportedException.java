/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * ClawbackNotSupportedException
 * 
 * Canceling winnings is not supported
 *
 */
public class ClawbackNotSupportedException extends ApiKnownException {

	private static final long serialVersionUID = -8019924962773879961L;
	private static final String ERROR_CODE = "ClawbackNotSupported";
	
	public ClawbackNotSupportedException(String message) {
		super(ERROR_CODE, message);
	}
	
	public ClawbackNotSupportedException(String message,  Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
