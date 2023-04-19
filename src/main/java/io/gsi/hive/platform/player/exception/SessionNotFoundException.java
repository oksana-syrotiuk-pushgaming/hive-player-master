/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class SessionNotFoundException extends ApiKnownException {

	private static final long serialVersionUID = 7117988473598395476L;
	private static final String ERROR_CODE = "SessionNotFound";

	public SessionNotFoundException(String message) {
		super(ERROR_CODE, message);
	}

	public SessionNotFoundException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}
}
