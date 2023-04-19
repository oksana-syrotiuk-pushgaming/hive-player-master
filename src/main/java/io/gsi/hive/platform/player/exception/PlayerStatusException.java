/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class PlayerStatusException extends ApiKnownException {

	private static final long serialVersionUID = 3661321206703889292L;
	private static final String ERROR_CODE = "PlayerStatus";

	public PlayerStatusException(String message) {
		super(ERROR_CODE, message);
	}

	public PlayerStatusException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}
	
	public PlayerStatusException(String message, Throwable throwable) {
		super(ERROR_CODE, message, throwable);
	}
}
