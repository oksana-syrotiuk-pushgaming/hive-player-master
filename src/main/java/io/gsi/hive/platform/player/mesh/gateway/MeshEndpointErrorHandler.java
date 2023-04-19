package io.gsi.hive.platform.player.mesh.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpointErrorHandler;
import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Loggable
@Component
public class MeshEndpointErrorHandler extends ObjectMapperHttpEndpointErrorHandler<ExceptionResponse>{

	public MeshEndpointErrorHandler(ObjectMapper objectMapper) {
		super(ExceptionResponse.class);
		setObjectMapper(objectMapper);
	}

	@Override
	protected void throwError(ExceptionResponse exceptionResponse, int statusCode) {
		/*When passing the response msg through, bear in mind that it could be null */
		if (exceptionResponse.getCode().equals("AuthorizationException")){
			throw new AuthorizationException("Could not authenticate: " + exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
			throw new ApiAuthorizationException("Could not authenticate: " + exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("TxnNotFoundException")) {
			throw new TxnNotFoundException(exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("PlayerNotFoundException")) {
			throw new AuthorizationException("Player not found: " + exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("TxnFailedException")) {
			throw new TxnFailedException(exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("InsufficientFundsException")) {
			throw new InsufficientFundsException(exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("PlayerLimitException")) {
			throw new PlayerLimitException(exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("PlayerStatusException")) {
			throw new PlayerStatusException(exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("TxnTombstoneException")) {
			throw new TxnFailedException("Txn Tombstone: " + exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("MandatoryGameBreakException")) {
			throw new MandatoryGameBreakException(exceptionResponse.getMsg(),
					exceptionResponse.getExtraInfo());
		} else if (exceptionResponse.getCode().equals("BadRequestException")) {
			throw new BadRequestException(exceptionResponse.getMsg());
		} else {
			/**Possible MeshExceptions caught here:
			 * 400/ConstraintViolation
			 * 501/ClawbackNotSupported,GameTxnStatusNotSupported
			 * 504/GatewayTimeout
			 * 409/InvalidState
			 * 404/PlayNF, GameNF
			 * 503/ServiceUnavailable
			 * 500/ISE
			 * */
			throw new ApiUnexpectedException(
					"Unexpected exception from mesh: " +exceptionResponse.getCode() +", msg: " + exceptionResponse.getMsg() + ", Code: " +statusCode,
					exceptionResponse.getExtraInfo());
		}
	}

	@Override
	protected void throwError(int statusCode) {
		throw new ApiUnexpectedException("Could not map error response, Code: " + statusCode);
	}

}
