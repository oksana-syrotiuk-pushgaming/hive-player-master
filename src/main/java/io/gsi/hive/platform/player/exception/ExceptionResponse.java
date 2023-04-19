/*
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ExceptionResponse {

	private String code;
	private String msg;
	private String reqId;
	private Map<String, Object> extraInfo;

	public ExceptionResponse(String code, String msg, String debug, String reqId, Map<String, Object> extraInfo) {
		this();
		this.code = code;
		this.msg = msg;
		this.reqId = reqId;
		this.extraInfo = extraInfo;
	}

	public ExceptionResponse() {
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getReqId() {
		return reqId;
	}

	public void setReqId(String reqId) {
		this.reqId = reqId;
	}
	
	public Map<String, Object> getExtraInfo(){
		return extraInfo;
	}
	
	public void setExtraInfo(Map<String, Object> extraInfo) {
		this.extraInfo = extraInfo;
	}
	

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExceptionResponse{code=‘").append(code).append("’, msg=‘").append(msg).append("’, reqId=‘")
				.append(reqId).append("', extraInfo='").append(extraInfo).append("'}");
		return builder.toString();
	}

}
