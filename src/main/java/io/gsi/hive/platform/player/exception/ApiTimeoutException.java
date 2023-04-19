/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * ApiTimeoutException
 * 
 * socket timeout
 *
 */
public class ApiTimeoutException extends ApiUnknownException {

	private static final long serialVersionUID = 1569712248330835308L;
	private static final String ERROR_CODE = "ApiTimeout";
	
	public ApiTimeoutException(String message) {
		super(ERROR_CODE, message);
	}

	public ApiTimeoutException(String message,  Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}
	
	public ApiTimeoutException(String message, Throwable t) {
		super(ERROR_CODE, message, t);
	}

}
