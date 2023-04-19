/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * Base class for exceptions arising from communication with an iGP API
 */
public abstract class ApiException extends PlatformException {

	private static final long serialVersionUID = 825022076702751327L;
	private boolean known;

	public ApiException(String code,String message,boolean known) {
		super(code,message);
		this.known = known;
	}

	public ApiException(String code, String message, boolean known, Map<String, Object> extraInfo) {
		super(code,message, extraInfo);
		this.known = known;
	}

	public ApiException(String code,String message,boolean known, Throwable throwable) {
		super(code,message, throwable);
		this.known = known;
	}

	public ApiException(String code,String message,boolean display,
			String displayMsg,boolean known) {
		super(code,message,display,displayMsg);
		this.known = known;
	}

	public boolean isKnown() {
		return known;
	}

}
