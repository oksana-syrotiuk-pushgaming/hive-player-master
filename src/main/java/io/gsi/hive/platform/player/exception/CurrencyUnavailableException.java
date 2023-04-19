/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * Exception thrown by rhino platform when the requested currency is not available
 */
public class CurrencyUnavailableException extends PlatformException {

	private static final long serialVersionUID = 2654157541906737848L;
	private static final String ERROR_CODE = "CurrencyUnavailable";

	public CurrencyUnavailableException(String message) {
		super(ERROR_CODE, message);
	}

	public CurrencyUnavailableException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
