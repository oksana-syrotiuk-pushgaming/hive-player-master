/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * Authorization exception from the IGP
 */
public class ApiAuthorizationException extends ApiKnownException {

	private static final long serialVersionUID = 3403222689975726038L;
	private static final String ERROR_CODE = "ApiAuthorizationException";

	public ApiAuthorizationException(String message) {
		super(ERROR_CODE, message);
	}

	public ApiAuthorizationException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

	public ApiAuthorizationException(String message, Throwable throwable) {
		super(ERROR_CODE, message, throwable);
	}
}
