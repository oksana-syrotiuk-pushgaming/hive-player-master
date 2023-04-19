package io.gsi.hive.platform.player.registry.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.rmi.registry.Registry;

@Service
public class RegistryEndpoint extends ObjectMapperHttpEndpoint {

    public RegistryEndpoint(@Qualifier("registryRestTemplate") RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        setRestTemplate(restTemplate);
        setObjectMapper(objectMapper);
    }
}
