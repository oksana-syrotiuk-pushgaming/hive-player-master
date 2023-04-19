package io.gsi.hive.platform.player.registry;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.registry.endpoint.RegistryEndpoint;
import io.gsi.hive.platform.player.registry.txn.IGPCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import javax.validation.*;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegistryGateway {
    private final RegistryEndpoint registryEndpoint;
    private final String hiveRegistryServiceName;
    private final String hiveRegistryApiKey;

    public RegistryGateway(
            RegistryEndpoint registryEndpoint,
            @Value("${endpoint.registry.serviceName:hive-registry-service-v1:9004}") String hiveRegistryServiceName,
            @Value("${endpoint.registry.apiKey}") String hiveRegistryApiKey
    ) {
        this.registryEndpoint = registryEndpoint;
        this.hiveRegistryServiceName = hiveRegistryServiceName;
        this.hiveRegistryApiKey = hiveRegistryApiKey;
    }

    @Cacheable(cacheNames = CacheConfig.GAME_ID_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
    public <T> T getConfig(String type, String tagKeys, String tagValues, Class<T> responseClass) {
        return getConfigUncached(type, tagKeys, tagValues, responseClass);
    }

    /**
     * This is a cacheable configuration with a list of IGP codes that are not allowed
     * to have the transactions' status forced to arbitrary status.
     * @return IGPCodes contains the list of IGP codes not allowed to have the status forced.
     */
    @Cacheable(cacheNames = CacheConfig.IGP_CODES_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
    public IGPCodes getConfigIgpCodes() {
        try {
            return getConfigUncached("IGP_CODE", "configType", "disallowForceStatusConfig", IGPCodes.class);
        } catch (RuntimeException e) {
            if(e.getMessage().contains("UnrecognizedPropertyException")){
                throw new InvalidStateException(
                        String.format("Unreadable request, some fields are incorrect. Original error message:<< %s >>", e.getMessage())
                );
            }
            throw e;
        }
    }

    public <T> T getConfigUncached(String type, String tagKeys, String tagValues, Class<T> responseClass) {
        T returnValue = registryEndpoint.send(
                    "http://{hiveRegistryServiceName}/hive/s2s/platform/registry/v1/config-merged?type={type}&tagKeys={tagKeys}&tagValues={tagValues}",
                    HttpMethod.GET,
                    Optional.empty(),
                    Optional.of(responseClass),
                    Optional.of(getHeaders()),
                    hiveRegistryServiceName,
                    type,
                    tagKeys,
                    tagValues
            ).orElseThrow(() -> new NotFoundException("No valid configuration found"));
        validate(returnValue);
        return returnValue;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("RegistryService-API-Key", hiveRegistryApiKey);
        return httpHeaders;
    }

    private <T> void validate(T toValidate) {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(toValidate);
        if (!constraintViolations.isEmpty()) {
            throw toInternalServerException(new ConstraintViolationException(constraintViolations));
        }
    }

    private InternalServerException toInternalServerException(ConstraintViolationException constraintViolationException) {
        Set<ConstraintViolation<?>> constraintViolations = constraintViolationException.getConstraintViolations();
        String constraintViolationsString = constraintViolations.stream()
                .map((constraintViolation) -> String.format("%s: %s", constraintViolation.getPropertyPath(), constraintViolation.getMessage()))
                .collect(Collectors.joining(", "));
        String exceptionMessage = String.format("constraint violations: %s", constraintViolationsString);
        return new InternalServerException(exceptionMessage);
    }
}
