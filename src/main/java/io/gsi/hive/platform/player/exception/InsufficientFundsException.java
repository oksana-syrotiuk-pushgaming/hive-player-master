/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class InsufficientFundsException extends ApiKnownException {

	private static final long serialVersionUID = 4504817873372084414L;
	private static final String ERROR_CODE = "InsufficientFunds";

	public InsufficientFundsException(String message) {
		super(ERROR_CODE, message);
	}

	public InsufficientFundsException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

	public InsufficientFundsException(String message, Throwable throwable) {
		super(ERROR_CODE, message, throwable);
	}
}
