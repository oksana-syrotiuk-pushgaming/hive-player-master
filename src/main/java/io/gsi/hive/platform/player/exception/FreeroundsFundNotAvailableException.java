package io.gsi.hive.platform.player.exception;


import java.util.Map;

public class FreeroundsFundNotAvailableException extends ApiKnownException {

  private static final String ERROR_CODE = "FreeroundsFundNotAvailable";

  public FreeroundsFundNotAvailableException(String message) {
    super(ERROR_CODE, message);
  }

  public FreeroundsFundNotAvailableException(String message, Map<String, Object> extraInfo) {
    super(ERROR_CODE, message, extraInfo);
  }
}
