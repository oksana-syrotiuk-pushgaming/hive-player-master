package io.gsi.hive.platform.player.demo.gateway;

import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpointErrorHandler;
import io.gsi.hive.platform.player.exception.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DemoWalletEndpointErrorHandler extends ObjectMapperHttpEndpointErrorHandler<ExceptionResponse>{

	public DemoWalletEndpointErrorHandler(ObjectMapper objectMapper) {
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
		} else if (exceptionResponse.getCode().equals("PlayerNotFound")) {
			throw new ApiAuthorizationException("Player not found: " + exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("TxnFailed")) {
			throw new TxnFailedException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("InsufficientFunds")) {
			throw new InsufficientFundsException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("PlayerLimit")) {
			throw new PlayerLimitException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("PlayerStatus")) {
			throw new PlayerStatusException(exceptionResponse.getMsg());
		} else if (exceptionResponse.getCode().equals("TxnTombstone")) {
			throw new TxnFailedException("Txn Tombstone: " + exceptionResponse.getMsg());
		}  else {
			/**Possibilities:
			 * 409/InvalidState
			 * 501/UnsupportedOperation
			 * 404/NotFound
			 *
			 * */
			throw new ApiUnexpectedException("Unexpected exception from demoWallet: " +exceptionResponse.getCode() +", msg: " + exceptionResponse.getMsg() + ", Code: " +statusCode);
		}
	}

	@Override
	protected void throwError(int statusCode) {
		throw new ApiUnexpectedException("could not map error response, Code: " + statusCode);
	}

}
