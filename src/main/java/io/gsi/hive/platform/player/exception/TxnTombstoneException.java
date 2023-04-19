package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class TxnTombstoneException extends PlatformException {

	private static final long serialVersionUID = 2453144501650017293L;
	private static final String ERROR_CODE = "TxnTombstone";
	
	public TxnTombstoneException(String message) {
		super(ERROR_CODE, message);
	}

	public TxnTombstoneException(String message, Map<String, Object> extraInfo) {
		super(ERROR_CODE, message, extraInfo);
	}

}
