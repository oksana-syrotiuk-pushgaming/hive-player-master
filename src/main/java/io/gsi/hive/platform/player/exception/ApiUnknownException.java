/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * An exception communicating with the iGP wallet API where we don't know if the txn was successfully processed by the
 * iGP or not.  Examples include timeout receiving the response from the API,  no response returned from the API, and
 * internal service errors returned from the API.  For these types of exceptions it is necessary to either retry the
 * txn against the API or send a cancel to ensure the original txn is cancelled, since we don't know the status of the txn
 * on the iGP side.
 */
public class ApiUnknownException extends ApiException {

	private static final long serialVersionUID = -3914580649952589948L;
	
	public ApiUnknownException(String errorCode, String message) {
		super(errorCode, message, false);
	}

	public ApiUnknownException(String errorCode, String message, Map<String, Object> extraInfo) {
		super(errorCode, message, false, extraInfo);
	}
	
	public ApiUnknownException(String errorCode, String message, Throwable t) {
		super(errorCode, message, false, t);
	}

}
