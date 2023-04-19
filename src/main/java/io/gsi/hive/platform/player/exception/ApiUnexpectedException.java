/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * ApiUnexpectedException
 * 
 * Unexpected exception communicating with API
 *
 */
public class ApiUnexpectedException extends ApiUnknownException {

	private static final long serialVersionUID = 1569712248330835308L;
	private static final String ERROR_CODE = "ApiUnexpected";
	
	public ApiUnexpectedException(String message) {
		super(ERROR_CODE, message);
	}

	public ApiUnexpectedException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}
	
	public ApiUnexpectedException(String message, Throwable throwable) {
		super(ERROR_CODE, message, throwable);
	}

}
