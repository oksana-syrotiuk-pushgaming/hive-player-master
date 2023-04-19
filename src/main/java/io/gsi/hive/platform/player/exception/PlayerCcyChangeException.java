/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class PlayerCcyChangeException extends ApiKnownException {

	private static final long serialVersionUID = -7983837643500313058L;
	private static final String ERROR_CODE = "PlayerCcyChange";

	public PlayerCcyChangeException(String message) {
		super(ERROR_CODE, message);
	}

	public PlayerCcyChangeException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
