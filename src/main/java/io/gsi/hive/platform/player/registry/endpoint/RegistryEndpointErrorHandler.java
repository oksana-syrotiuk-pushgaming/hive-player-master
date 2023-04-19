package io.gsi.hive.platform.player.registry.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.ForbiddenException;
import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.ServiceUnavailableException;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpointErrorHandler;
import io.gsi.hive.platform.player.registry.exception.RegistryExceptionResponse;
import org.springframework.stereotype.Component;

@Component
public class RegistryEndpointErrorHandler extends ObjectMapperHttpEndpointErrorHandler<RegistryExceptionResponse> {
    public RegistryEndpointErrorHandler(ObjectMapper objectMapper) {
        super(RegistryExceptionResponse.class);
        super.setObjectMapper(objectMapper);
    }

    @Override
    protected void throwError(RegistryExceptionResponse registryExceptionResponse, int statusCode) {
        String code = registryExceptionResponse.getCode();
        String message = registryExceptionResponse.getMessage();
        switch (code) {
            case "BadRequest":
                throw new BadRequestException(message);
            case "Forbidden":
                throw new ForbiddenException(message);
            case "ServiceUnavailable":
                throw new ServiceUnavailableException(message);
            default:
                throw new InternalServerException(String.format("could not map error response, code: %s statusCode: %d message: %s", code, statusCode, message));
        }
    }

    @Override
    protected void throwError(int statusCode) {
        throw new InternalServerException(String.format("could not map error response, statusCode: %d", statusCode));
    }
}
