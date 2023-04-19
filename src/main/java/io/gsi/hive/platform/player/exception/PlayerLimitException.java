/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class PlayerLimitException extends ApiKnownException {

	private static final long serialVersionUID = 4504817873372084414L;
	private static final String ERROR_CODE = "PlayerLimit";

	public PlayerLimitException(String message) {
		super(ERROR_CODE, message);
	}

	public PlayerLimitException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

	public PlayerLimitException(String message, Throwable throwable) {
		super(ERROR_CODE, message, throwable);
	}
}
