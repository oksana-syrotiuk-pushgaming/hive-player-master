/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class GuestPlayerIdClashException extends ApiKnownException {

	private static final long serialVersionUID = -4776433319305020069L;
	private static final String ERROR_CODE = "GuestPlayerIdClash";

	public GuestPlayerIdClashException(String message) {
		super(ERROR_CODE, message);
	}

	public GuestPlayerIdClashException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
