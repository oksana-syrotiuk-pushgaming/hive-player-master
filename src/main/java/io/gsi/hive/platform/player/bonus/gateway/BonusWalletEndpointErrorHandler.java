package io.gsi.hive.platform.player.bonus.gateway;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpointErrorHandler;
import io.gsi.hive.platform.player.exception.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BonusWalletEndpointErrorHandler extends ObjectMapperHttpEndpointErrorHandler<ExceptionResponse>{

	public BonusWalletEndpointErrorHandler(ObjectMapper objectMapper) {
		super(ExceptionResponse.class);
		setObjectMapper(objectMapper);
	}

	@Override
	protected void throwError(ExceptionResponse exceptionResponse, int statusCode) {

		/*When passing the response msg through, bear in mind that it could be null */
		if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
			throw new ApiAuthorizationException("Could not authenticate: " + exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("TxnNotFound")) {
			throw new TxnNotFoundException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("TxnFailed")) {
			throw new TxnFailedException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("InsufficientFunds")) {
			throw new InsufficientFundsException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("TxnTombstone")) {
			throw new TxnFailedException("Txn Tombstone: " + exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("InvalidState")) {
			throw new InvalidStateException("Invalid State : " + exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("FreeroundsFundNotAvailable")){
			throw new FreeroundsFundNotAvailableException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("FundNotFound")) {
			throw new NotFoundException("Fund not found : " + exceptionResponse.getMsg());
		} else {
			/**Possible BonusWallet Exceptions caught here:
			 * 400/BadRequest,ConstraintViolation
			 * 404/NotFound
			 * 412/FundNotFound
			 * 501/UnsupportedOperation
			 * 409/InvalidState
			 * 500/ISE
			 * */
			throw new ApiUnexpectedException(
					"Unexpected exception from bonusWallet: " + exceptionResponse.getCode() + ", msg: "
							+ exceptionResponse.getMsg() + ", Code: " + statusCode);
		}
	}

	@Override
	protected void throwError(int statusCode) {
		throw new ApiUnexpectedException("could not map error response, Code: " + statusCode);
	}

}
