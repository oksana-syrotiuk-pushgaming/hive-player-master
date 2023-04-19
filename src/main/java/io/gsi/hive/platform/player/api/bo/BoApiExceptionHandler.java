package io.gsi.hive.platform.player.api.bo;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.WebAppException;
import io.gsi.hive.platform.player.exception.ApiAuthorizationException;
import javax.validation.ConstraintViolationException;

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

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.ServiceUnavailableException;
import io.gsi.commons.logging.Loggable;
import io.gsi.commons.monitoring.ExceptionMonitorService;
import io.gsi.hive.platform.player.exception.ExceptionResponse;
import io.gsi.hive.platform.player.exception.PlatformException;

@ControllerAdvice("io.gsi.hive.platform.player.api.bo")
@Component
@Loggable
public class BoApiExceptionHandler extends ResponseEntityExceptionHandler
{
	private static final Log logger = LogFactory.getLog(BoApiExceptionHandler.class);
	private final ExceptionMonitorService exceptionMonitorService;

	public BoApiExceptionHandler(ExceptionMonitorService exceptionMonitorService) {
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
		return handleException(new AuthorizationException(e.getMessage()));
	}

	@ExceptionHandler(value={IllegalArgumentException.class})
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse handleIllegalArgumentException(IllegalArgumentException e) {
		return handleException(new BadRequestException(e.getMessage()));
	}

	@ExceptionHandler(value={ConstraintViolationException.class})
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse handleConstraintException(ConstraintViolationException e) {
		return handleException(new BadRequestException(
				e.getConstraintViolations().toString()));
	}

	@ExceptionHandler(value={ResourceAccessException.class})
	public ResponseEntity<ExceptionResponse> handleResourceAccessException(ResourceAccessException rae) {
		HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
		return new ResponseEntity<ExceptionResponse>(createExceptionResponse(rae),statusCode);
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	@ResponseStatus(value=HttpStatus.SERVICE_UNAVAILABLE)
	public @ResponseBody ExceptionResponse handleServiceUnavailableException(Exception e) {
		return createExceptionResponse(e);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ExceptionResponse handleInternalServerException(Exception e) {
		return handleException(new InternalServerException(e.getMessage()));
	}

	@ExceptionHandler(value={InvalidStateException.class})
	@ResponseStatus(value=HttpStatus.CONFLICT)
	public @ResponseBody ExceptionResponse handleInvalidStateException(Exception e) {
		return handleException(e);
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
		} else if (ex instanceof WebAppException) {
			final var webEx = (WebAppException)ex;
			resp.setCode(webEx.getClass().getSimpleName().replace("Exception",""));
			resp.setMsg(webEx.getMessage());
			resp.setExtraInfo(webEx.getExtraInfo());
		} else {
			resp.setCode(ex.getClass().getSimpleName().replace("Exception",""));
		}
		resp.setReqId(Thread.currentThread().getName());
		return resp;
	}
}


