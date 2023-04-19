/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import java.util.Map;

/**
 * Base class for HIVE platform exceptions
 */
public class PlatformException extends RuntimeException {

	private static final long serialVersionUID = -8531257682843459706L;
	private String code;
	private boolean display;
	private String displayMsg;
	private Map<String, Object> extraInfo;

	public PlatformException(String code, String message, Throwable throwable) {
		super(message, throwable);
		this.code = code;
		this.display = false;
	}

	public PlatformException(String code,String message) {
		super(message);
		this.code = code;
		this.display = false;
	}
	
	public PlatformException(String code,String message, Map<String, Object> extraInfo) {
		super(message);
		this.code = code;
		this.display = false;
		this.extraInfo = extraInfo;
	}

	public PlatformException(String code,String message,boolean display,String displayMsg) {
		super(message);
		this.code = code;
		this.display = display;
		this.displayMsg = displayMsg;
	}

	public String getCode() {
		return code;
	}

	public boolean isDisplay() {
		return display;
	}

	public String getDisplayMsg() {
		return displayMsg;
	}
	
	public Map<String, Object> getExtraInfo() {
		return extraInfo;
	}

}
