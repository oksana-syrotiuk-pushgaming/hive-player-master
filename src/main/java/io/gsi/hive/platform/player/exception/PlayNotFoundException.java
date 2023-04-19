/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * Exeption thrown by rhino platfrom when a given playId cannot be found
 */
public class PlayNotFoundException extends PlatformException {

	private static final long serialVersionUID = -4761167385722115787L;
	private static final String ERROR_CODE = "PlayNotFound";

	public PlayNotFoundException() {
		super(ERROR_CODE,"playId not found");
	}

	public PlayNotFoundException(Map<String, Object> extraInfo) {
		super(ERROR_CODE,"playId not found", extraInfo);
	}

	public PlayNotFoundException(Long playId) {
		super(ERROR_CODE,String.format("playId=%s not found",playId));
	}

}
