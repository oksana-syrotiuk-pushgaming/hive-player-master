/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class InvalidRouteException extends ApiKnownException {

	private static final long serialVersionUID = 8186912206882325720L;
	private static final String ERROR_CODE = "InvalidRoute";
	
	public InvalidRouteException(String message) {
		super(ERROR_CODE, message);
	}

	public InvalidRouteException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

	public InvalidRouteException(String message, Throwable throwable) {
		super(ERROR_CODE, message, throwable);
	}
}
