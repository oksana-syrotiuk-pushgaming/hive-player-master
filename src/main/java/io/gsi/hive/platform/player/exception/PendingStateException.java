/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * The open game play is in a pending state and game play cannot continue
 * on this game until the state has moved out of the pending state (for example
 * after reconciliation)
 */
public class PendingStateException extends PlatformException {

	private static final long serialVersionUID = -3630926621450001283L;
	private static final String ERROR_CODE = "PendingState";

	public PendingStateException(String message) {
		super(ERROR_CODE, message);
	}

	public PendingStateException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
