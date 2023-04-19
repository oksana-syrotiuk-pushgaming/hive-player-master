/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * An exception communicating with the iGP wallet API which we know did not result in the txn getting successfully
 * processed.  Examples include timeout connecting to the API, insufficient funds returned from the API, player status
 * error returned from the API and authorization error returned from API.  For this type of error there is no need to retry
 * or send a cancel of the original txn, since we know that the API did not process it.
 */
public class ApiKnownException extends ApiException {

	private static final long serialVersionUID = -3914580649952589948L;

	public ApiKnownException(String code, String message) {
		super(code, message, true);
	}

	public ApiKnownException(String code, String message, Map<String, Object> extraInfo) {
		super(code, message, true, extraInfo);
	}

	public ApiKnownException(String code, String message, Throwable throwable) {
		super(code, message, true, throwable);
	}

	public ApiKnownException(String code, String message, String displayMsg) {
		super(code, message, true, displayMsg, true);
	}
}
