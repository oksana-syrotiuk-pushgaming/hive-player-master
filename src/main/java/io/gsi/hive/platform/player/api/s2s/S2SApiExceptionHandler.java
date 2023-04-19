package io.gsi.hive.platform.player.api.s2s;

import io.gsi.commons.exception.*;
import io.gsi.commons.logging.Loggable;
import io.gsi.commons.monitoring.ExceptionMonitorService;
import io.gsi.hive.platform.player.exception.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.net.SocketTimeoutException;

@ControllerAdvice("io.gsi.hive.platform.player.api.s2s")
@Component
@Loggable
public class S2SApiExceptionHandler extends ResponseEntityExceptionHandler
{
	private static final Log logger = LogFactory.getLog(S2SApiExceptionHandler.class);
	private final ExceptionMonitorService exceptionMonitorService;

	public S2SApiExceptionHandler(ExceptionMonitorService exceptionMonitorService) {
		this.exceptionMonitorService = exceptionMonitorService;
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
			Object body, HttpHeaders headers, HttpStatus status,
			WebRequest request) {
		ExceptionResponse exceptionResponse;
		if (status == HttpStatus.BAD_REQUEST) {
			BadRequestException be = new BadRequestException(
					ex.getClass().getSimpleName() + ": " + ex.getMessage());
			exceptionResponse = handleException(be);
		} else {
			exceptionResponse = handleException(ex);
		}
		return new ResponseEntity<>(exceptionResponse,headers,status);
	}

	@ExceptionHandler(value={AuthorizationException.class, ApiAuthorizationException.class})
	@ResponseStatus(value=HttpStatus.UNAUTHORIZED)
	public @ResponseBody ExceptionResponse handleAuthorizationException(Exception e) {
		return handleException(e);
	}

	/**Include violations if they are present*/
	@ExceptionHandler(value={ConstraintViolationException.class})
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse handleConstraintException(ConstraintViolationException e) {
		return handleException(new BadRequestException(
				e.getConstraintViolations().toString()));
	}

	@ExceptionHandler(value={BadRequestException.class})
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse handleBadRequestException(Exception e) {
		return handleException(e);
	}

	@ExceptionHandler(value={PlayNotFoundException.class, TxnNotFoundException.class, NotFoundException.class, SessionNotFoundException.class})
	@ResponseStatus(value=HttpStatus.NOT_FOUND)
	public @ResponseBody ExceptionResponse handleNotFoundException(Exception e) {
		return handleException(e);
	}

	@ExceptionHandler(value={UnsupportedOperationException.class, ClawbackNotSupportedException.class})
	@ResponseStatus(value=HttpStatus.NOT_IMPLEMENTED)
	public @ResponseBody ExceptionResponse handleNotSupportedException(Exception e) {
		return createExceptionResponse(e);
	}

	@ExceptionHandler(value={ApiKnownException.class, InvalidStateException.class,PendingStateException.class})
	@ResponseStatus(value=HttpStatus.CONFLICT)
	public @ResponseBody ExceptionResponse handleInvalidStateException(Exception e) {
		return handleException(e);
	}

	@ExceptionHandler(value={InsufficientFundsException.class, CurrencyUnavailableException.class, TxnFailedException.class, TxnTombstoneException.class})
	@ResponseStatus(value=HttpStatus.PRECONDITION_FAILED)
	public @ResponseBody ExceptionResponse handleInsufficientFundsException(Exception e) {
		return handleException(e);
	}

	@ExceptionHandler(value={PlayerLimitException.class, PlayerStatusException.class, MandatoryGameBreakException.class})
	@ResponseStatus(value=HttpStatus.FORBIDDEN)
	public @ResponseBody ExceptionResponse handlePlayerLimitException(Exception e) {
		return handleException(e);
	}

	@ExceptionHandler(value={ForbiddenException.class})
	@ResponseStatus(value=HttpStatus.FORBIDDEN)
	public @ResponseBody ExceptionResponse handleForbiddenExceptionResponse(Exception e) {
		return handleException(e);
	}

	/*
	 * Handle resource access exceptions, specifically a timeout
	 */
	@ExceptionHandler(value={ResourceAccessException.class})
	public ResponseEntity<ExceptionResponse> handleResourceAccessException(ResourceAccessException rae) {
		HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
		if (rae.getCause() instanceof SocketTimeoutException) {
			statusCode = HttpStatus.GATEWAY_TIMEOUT;
			ApiTimeoutException ate = new ApiTimeoutException(rae.getMessage(),rae);
			return new ResponseEntity<ExceptionResponse>(createExceptionResponse(ate),statusCode);
		} else {
			return new ResponseEntity<ExceptionResponse>(createExceptionResponse(rae),statusCode);
		}
	}

	/*
	 * Handle service unavailable exceptions
	 */
	@ExceptionHandler(ServiceUnavailableException.class)
	@ResponseStatus(value=HttpStatus.SERVICE_UNAVAILABLE)
	public @ResponseBody ExceptionResponse handleServiceUnavailableException(Exception e) {
		return createExceptionResponse(e);
	}

	@ExceptionHandler(WebAppException.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ExceptionResponse handleWebAppException(WebAppException e) {
		return handleException(new InternalServerException(e.getMessage(),
				e.getExtraInfo()));
	}

	@ExceptionHandler(PlatformException.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ExceptionResponse handlePlatformException(PlatformException e) {
		return handleException(new InternalServerException(e.getMessage(),
				e.getExtraInfo()));
	}

	/**Catch all*/
	@ExceptionHandler(Exception.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ExceptionResponse handleInternalServerException(Exception e) {
		return handleException(new InternalServerException(e.getMessage()));
	}

	private ExceptionResponse handleException(Exception e) {
		exceptionMonitorService.monitorException(e);
		return createExceptionResponse(e);
	}

	private ExceptionResponse createExceptionResponse(Exception ex) {
		logger.info(String.format("handling exception=%s, msg=%s",
				ex.getClass().getSimpleName(),ex.getMessage()));
		ExceptionResponse resp = new ExceptionResponse();
		if (ex instanceof PlatformException) {
			PlatformException pex = (PlatformException) ex;
			resp.setCode(pex.getCode());
			if (pex.isDisplay()) {
				resp.setMsg(pex.getDisplayMsg());
			}
			else{
				resp.setMsg(pex.getMessage());
			}
			resp.setExtraInfo(pex.getExtraInfo());
		} else if(ex instanceof WebAppException) {

			final var wex = (WebAppException)ex;
			resp.setCode(wex.getClass().getSimpleName().replace("Exception",""));
			resp.setMsg(wex.getMessage());
			resp.setExtraInfo(wex.getExtraInfo());

		} else {
			resp.setCode(ex.getClass().getSimpleName().replace("Exception",""));
			//TODO: Should this only be under integration profile?
			resp.setMsg(ex.getMessage());
		}
		resp.setReqId(Thread.currentThread().getName());
		return resp;
	}
}


