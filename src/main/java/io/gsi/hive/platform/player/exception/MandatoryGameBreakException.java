package io.gsi.hive.platform.player.exception;

import java.util.Map;

public class MandatoryGameBreakException extends ApiKnownException {
  private static final String ERROR_CODE = "MandatoryGameBreak";
  
  
  public MandatoryGameBreakException(String message) {
    super(ERROR_CODE, message);
  }

  public MandatoryGameBreakException(String message, Map<String, Object> extraInfo) {
    super(ERROR_CODE, message, extraInfo);
  }
  
}
