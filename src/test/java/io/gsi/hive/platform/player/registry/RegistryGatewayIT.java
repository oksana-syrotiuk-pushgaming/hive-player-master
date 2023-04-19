package io.gsi.hive.platform.player.registry;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.registry.endpoint.RegistryEndpoint;
import io.gsi.hive.platform.player.registry.gameInfo.ValidGameIds;
import io.gsi.hive.platform.player.registry.txn.IGPCodes;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
public class RegistryGatewayIT {
    private final String REGISTRY_CONFIG_URL = "http://{hiveRegistryServiceName}/hive/s2s/platform/registry/v1/config-merged?type={type}&tagKeys={tagKeys}&tagValues={tagValues}";

    @MockBean
    private RegistryEndpoint registryEndpoint;

    @Autowired
    private RegistryGateway registryGateway;

    @Autowired
    @Qualifier(CacheConfig.GAME_ID_CACHE_NAME)
    private CaffeineCache gameIdCache;

    @Value("${endpoint.registry.gameId.cache.expirySeconds}")
    Integer gameIdCacheExpirySeconds;

    @Before
    public void setup() {
        gameIdCache.clear();
    }

    @Test
    public void givenValidResponse_whenGetConfig_thenConfigReturned() {
        ValidGameIds expected = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000)).build();
        doReturn(Optional.of(expected)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        ValidGameIds actual = registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class);
        assertThat(actual).isEqualTo(expected);
        verify(registryEndpoint, times(1)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
    }

    @Test
    public void givenDuplicateParameters_whenGetConfig_thenConfigReturnedFromCache() {
        ValidGameIds expected = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000)).build();
        doReturn(Optional.of(expected)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        ValidGameIds firstActual = registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class);
        assertThat(firstActual).isEqualTo(expected);
        ValidGameIds secondActual = registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class);
        assertThat(secondActual).isEqualTo(expected);
        verify(registryEndpoint, times(1)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
    }

    @Test
    public void givenDuplicateParametersAndExpiredCache_whenGetConfig_thenEndpointCalledTwice() throws Exception {
        ValidGameIds expected = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000)).build();
        doReturn(Optional.of(expected)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        ValidGameIds firstActual = registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class);
        assertThat(firstActual).isEqualTo(expected);
        Thread.sleep(gameIdCacheExpirySeconds * 1000 + 10);
        ValidGameIds secondActual = registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class);
        assertThat(secondActual).isEqualTo(expected);
        verify(registryEndpoint, times(2)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
    }

    @Test
    public void givenEmptyResponse_whenGetConfig_thenNotFoundExceptionThrown() {
        doReturn(Optional.empty()).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        assertThatThrownBy(() -> registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No valid configuration found");
    }

    @Test
    public void givenInvalidResponse_whenGetConfig_thenConstraintViolationExceptionThrown() {
        ValidGameIds invalid = ValidGameIds.builder().gameCodeToGameId(Map.of("testgame", 1000, "testgame2", 1000)).build();
        doReturn(Optional.of(invalid)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(ValidGameIds.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "GAME_ID",
                "configType",
                "gameIdConfig"
        );
        assertThatThrownBy(() -> registryGateway.getConfig("GAME_ID", "configType", "gameIdConfig", ValidGameIds.class))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("constraint violations: gameCodeToGameId: validator.gameIds.invalid");
    }

    @DisplayName("Verify that the list of IGP codes is returned correctly after response from the registry")
    @Test
    public void givenValidResponse_whenGetConfigIgpCodes_thenIGPCodesReturned() {
        var expectedIgpCodes = IGPCodes.builder().igpCodesList(List.of()).build();
        doReturn(Optional.of(expectedIgpCodes)).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(IGPCodes.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "IGP_CODE",
                "configType",
                "disallowForceStatusConfig"
        );
        var actualIgpCodes = registryGateway.getConfigIgpCodes();
        assertThat(actualIgpCodes).isEqualTo(expectedIgpCodes);
        verify(registryEndpoint, times(1)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(IGPCodes.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "IGP_CODE",
                "configType",
                "disallowForceStatusConfig"
        );
    }

    @DisplayName("Verify that InvalidStateException is thrown the response from the registry has unknown fields")
    @Test
    public void givenResponseWithUnknownFields_whenGetConfigIgpCodes_thenInvalidStateExceptionThrown() {
        doThrow(new RuntimeException("UnrecognizedPropertyException")).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(IGPCodes.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "IGP_CODE",
                "configType",
                "disallowForceStatusConfig"
        );
        assertThatThrownBy(() -> registryGateway.getConfigIgpCodes())
                .isInstanceOf(InvalidStateException.class)
                .isEqualToComparingFieldByField(new InvalidStateException(""))
                .hasMessage("Unreadable request, some fields are incorrect. Original error message:<< UnrecognizedPropertyException >>");
        verify(registryEndpoint, times(1)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(IGPCodes.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "IGP_CODE",
                "configType",
                "disallowForceStatusConfig"
        );
    }

    @DisplayName("Verify that when there is an exception sending to the registry it is propagated correctly")
    @Test
    public void givenExceptionSendingRequest_whenGetConfigIgpCodes_thenOriginalExceptionThrown() {
        doThrow(new RuntimeException("Other error")).when(registryEndpoint).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(IGPCodes.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "IGP_CODE",
                "configType",
                "disallowForceStatusConfig"
        );
        assertThatThrownBy(() -> registryGateway.getConfigIgpCodes())
                .isInstanceOf(RuntimeException.class)
                .isEqualToComparingFieldByField(new RuntimeException(""))
                .hasMessage("Other error");
        verify(registryEndpoint, times(1)).send(
                REGISTRY_CONFIG_URL,
                HttpMethod.GET,
                Optional.empty(),
                Optional.of(IGPCodes.class),
                Optional.of(getHeaders()),
                "hive-registry-service-v1:9004",
                "IGP_CODE",
                "configType",
                "disallowForceStatusConfig"
        );
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("RegistryService-API-Key", "registryTestApiKey");
        return httpHeaders;
    }
}
