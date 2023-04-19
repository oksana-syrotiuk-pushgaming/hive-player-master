package io.gsi.hive.platform.player.mesh.igpservicelocator;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.mesh.gateway.MeshGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IgpServiceLocationCachePopulator {

    private static final String MESH_NODE_IGP_PREFIX = "mesh-node-igp-";

    private final DiscoveryClient discoveryClient;
    private final MeshGateway gateway;

    @Autowired
    public IgpServiceLocationCachePopulator(DiscoveryClient discoveryClient, MeshGateway gateway) {
        this.discoveryClient = discoveryClient;
        this.gateway = gateway;
    }

    public Map<String, String> getIgpServiceCodes() {
        List<String> igpServiceNames = getIgpServiceNames();
        Map<String, String> igpServiceCodes = new HashMap<>();

        for (String igpServiceName : igpServiceNames) {
            String igpServiceCode = getIgpServiceCode(igpServiceName);
            List<String> supportedIgpCodes = getSupportedIgpCodesFromIgpNode(igpServiceCode);

            for (String supportedIgpCode : supportedIgpCodes) {
                if (igpServiceCodes.containsKey(supportedIgpCode)) {
                    String message = String.format("multiple igp match for igpCode=%s", supportedIgpCode);
                    log.error(message);
                    throw new InvalidStateException(message);
                }

                igpServiceCodes.put(supportedIgpCode, igpServiceCode);
            }
        }

        return igpServiceCodes;
    }

    private List<String> getIgpServiceNames() {
        List<String> allServices = discoveryClient.getServices();
        return allServices.stream()
                .filter(serviceName -> serviceName.startsWith(MESH_NODE_IGP_PREFIX))
                .collect(Collectors.toList());
    }

    private String getIgpServiceCode(String igpServiceUrl) {
        return igpServiceUrl.replace(MESH_NODE_IGP_PREFIX, "");
    }

    private List<String> getSupportedIgpCodesFromIgpNode(String igpCode) {
        SupportedIgpCodes supportedIgpCodes = null;

        try {
            supportedIgpCodes = gateway.getSupportedIgpCodes(igpCode);
        } catch (Exception e) {
            log.error("Error occurred while getting supported igp codes: {}", e.getMessage());
        }

        return supportedIgpCodes == null ? Collections.emptyList() : supportedIgpCodes.getIgpCodes();
    }
}
