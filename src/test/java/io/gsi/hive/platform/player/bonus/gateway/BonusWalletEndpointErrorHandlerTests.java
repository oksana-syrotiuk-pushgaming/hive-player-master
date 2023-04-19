package io.gsi.hive.platform.player.bonus.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.exception.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class BonusWalletEndpointErrorHandlerTests {

  private BonusWalletEndpointErrorHandler bonusWalletEndpointErrorHandler;

  @Before
  public void setup() {
    ObjectMapper objectMapper = new ObjectMapper();
    bonusWalletEndpointErrorHandler = new BonusWalletEndpointErrorHandler(objectMapper);
  }

  @Test(expected = InsufficientFundsException.class)
  public void givenExceptionResponse_whenThrowError_throwsInsufficientFundsException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("InsufficientFunds",
        "Error has occurred", "FundsException", "1", null);
    bonusWalletEndpointErrorHandler
        .throwError(exceptionResponse, HttpStatus.PRECONDITION_FAILED.value());
  }

  @Test(expected = TxnFailedException.class)
  public void givenTxnTombstoneExceptionResponse_whenThrowError_throwsTxnFailedException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("TxnTombstone",
        "TxnTombstoneException has occurred", "TxnTombstoneException", "1", null);
    bonusWalletEndpointErrorHandler
        .throwError(exceptionResponse, HttpStatus.PRECONDITION_FAILED.value());
  }

  @Test(expected = TxnFailedException.class)
  public void givenTxnFailedExceptionResponse_whenThrowError_throwsTxnFailedException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("TxnFailed",
        "TxnFailedException has occurred", "TxnFailedException", "1", null);
    bonusWalletEndpointErrorHandler.throwError(exceptionResponse, HttpStatus.CONFLICT.value());
  }

  @Test(expected = TxnNotFoundException.class)
  public void givenExceptionResponse_whenThrowError_throwsTxnNotFoundException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("TxnNotFound",
        "TxnNotFoundException has occurred", "TxnNotFoundException", "1", null);
    bonusWalletEndpointErrorHandler.throwError(exceptionResponse, HttpStatus.BAD_REQUEST.value());
  }

  @Test(expected = ApiAuthorizationException.class)
  public void givenExceptionResponse_whenThrowError_throwsApiAuthorizationException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("ApiAuthorizationException",
        "ApiAuthorizationException has occurred", "ApiAuthorizationException", "1", null);
    bonusWalletEndpointErrorHandler.throwError(exceptionResponse, HttpStatus.UNAUTHORIZED.value());
  }

  @Test(expected = ApiUnexpectedException.class)
  public void givenUnknownExceptionResponse_whenThrowError_throwsApiUnexpectedException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("ApiUnexpectedException",
        "ApiUnexpectedException has occurred", "ApiUnexpectedException", "1", null);
    bonusWalletEndpointErrorHandler.throwError(exceptionResponse, HttpStatus.CONFLICT.value());
  }

  @Test(expected = InvalidStateException.class)
  public void givenInvalidStateExceptionResponse_whenThrowError_throwsInvalidStateException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("InvalidState",
        "InvalidStateException has occurred", "InvalidStateException", "1", null);
    bonusWalletEndpointErrorHandler.throwError(exceptionResponse, HttpStatus.CONFLICT.value());
  }

  @Test(expected = NotFoundException.class)
  public void givenFundNotFoundExceptionResponse_whenThrowError_throwsNotFoundException() {
    ExceptionResponse exceptionResponse = new ExceptionResponse("FundNotFound",
        "FundNotFoundException has occurred", "FundNotFoundException", "1", null);
    bonusWalletEndpointErrorHandler.throwError(exceptionResponse, HttpStatus.NOT_FOUND.value());
  }

  @Test(expected = FreeroundsFundNotAvailableException.class)
  public void givenFreeroundsFundNotAvailableExceptionResponse_whenThrowError_throwsFreeroundsFundNotAvailableException(){
    ExceptionResponse exceptionResponse = new ExceptionResponse("FreeroundsFundNotAvailable",
        "FreeroundsFundNotAvailableException has occurred", "FreeroundsFundNotAvailable", "1", null);
    bonusWalletEndpointErrorHandler.throwError(exceptionResponse, HttpStatus.PRECONDITION_FAILED.value());
  }
}
