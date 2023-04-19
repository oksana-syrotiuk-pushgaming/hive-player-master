package io.gsi.hive.platform.player.mesh.igpservicelocator;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.mesh.gateway.DefaultMeshGateway;
import io.gsi.hive.platform.player.presets.IgpPresets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class IgpServiceLocationCachePopulatorTests {

    private static final String MESH_NODE_IGP_PREFIX = "mesh-node-igp-";
    private static final String TIKITUMBLE_SERVICE = "hive-game-tikitumble-service";
    private static final String HIVE_RNG = "hive-rng-service-v1";
    private static final String IGUANA_SERVICE = "iguana-service";
    private static final String HIVE_BACKOFFICE_SERVICE = "hive-back-office-service-v1";

    @InjectMocks
    private IgpServiceLocationCachePopulator igpServiceLocationCachePopulator;

    @Mock
    private DiscoveryClient discoveryClient;
    @Mock
    private DefaultMeshGateway gateway;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(discoveryClient.getServices())
                .thenReturn(
                        Arrays.asList(
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_IGUANA,
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_GECKO,
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_CHAMELEON,
                                TIKITUMBLE_SERVICE,
                                HIVE_RNG,
                                IGUANA_SERVICE,
                                HIVE_BACKOFFICE_SERVICE));

        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA))
                .thenReturn(new SupportedIgpCodes(Collections.singletonList(IgpPresets.IGPCODE_IGUANA)));
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_GECKO))
                .thenReturn(new SupportedIgpCodes(Collections.singletonList(IgpPresets.IGPCODE_GECKO)));
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_CHAMELEON))
                .thenReturn(new SupportedIgpCodes(Collections.singletonList(IgpPresets.IGPCODE_CHAMELEON)));
    }

    @Test
    public void givenMultipleServices_whenGetIgpServiceCodes_returnsIgpServiceCodes() {
        Map<String, String> expected = new HashMap<>();
        expected.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
        expected.put(IgpPresets.IGPCODE_GECKO, IgpPresets.IGPCODE_GECKO);
        expected.put(IgpPresets.IGPCODE_CHAMELEON, IgpPresets.IGPCODE_CHAMELEON);
        Map<String, String> igpServiceCodes = igpServiceLocationCachePopulator.getIgpServiceCodes();
        assertThat(igpServiceCodes).isEqualTo(expected);
        Mockito.verify(discoveryClient).getServices();
        Mockito.verify(gateway).getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA);
        Mockito.verify(gateway).getSupportedIgpCodes(IgpPresets.IGPCODE_GECKO);
        Mockito.verify(gateway).getSupportedIgpCodes(IgpPresets.IGPCODE_CHAMELEON);
    }

    private void multiTenancySetup() {
        when(discoveryClient.getServices())
                .thenReturn(
                        Arrays.asList(
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_IGUANA,
                                TIKITUMBLE_SERVICE,
                                HIVE_RNG,
                                IGUANA_SERVICE,
                                HIVE_BACKOFFICE_SERVICE));
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA))
                .thenReturn(
                        new SupportedIgpCodes(
                                Arrays.asList(
                                        IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_GECKO, IgpPresets.IGPCODE_CHAMELEON)));
        igpServiceLocationCachePopulator =
                new IgpServiceLocationCachePopulator(discoveryClient, gateway);
    }

    private void supportedIgpCodesNotFound() {
        when(discoveryClient.getServices())
                .thenReturn(
                        Arrays.asList(
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_IGUANA,
                                TIKITUMBLE_SERVICE,
                                HIVE_RNG,
                                IGUANA_SERVICE,
                                HIVE_BACKOFFICE_SERVICE));
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA))
                .thenThrow(NoSuchElementException.class);
        igpServiceLocationCachePopulator =
                new IgpServiceLocationCachePopulator(discoveryClient, gateway);
    }

    private void supportedIgpCodesNotFoundForOneOfIgpNodes() {
        when(discoveryClient.getServices())
                .thenReturn(
                        Arrays.asList(
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_IGUANA,
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_GECKO,
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_CHAMELEON,
                                TIKITUMBLE_SERVICE,
                                HIVE_RNG,
                                IGUANA_SERVICE,
                                HIVE_BACKOFFICE_SERVICE));
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA))
                .thenReturn(new SupportedIgpCodes(Collections.singletonList(IgpPresets.IGPCODE_IGUANA)));
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_GECKO))
                .thenThrow(NoSuchElementException.class);
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_CHAMELEON))
                .thenReturn(new SupportedIgpCodes(Collections.singletonList(IgpPresets.IGPCODE_CHAMELEON)));
        igpServiceLocationCachePopulator =
                new IgpServiceLocationCachePopulator(discoveryClient, gateway);
    }

    private void invalidMultiTenancySetup() {
        when(discoveryClient.getServices())
                .thenReturn(
                        Arrays.asList(
                                MESH_NODE_IGP_PREFIX + IgpPresets.IGPCODE_IGUANA,
                                TIKITUMBLE_SERVICE,
                                HIVE_RNG,
                                IGUANA_SERVICE,
                                HIVE_BACKOFFICE_SERVICE));
        when(gateway.getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA))
                .thenReturn(
                        new SupportedIgpCodes(
                                Arrays.asList(
                                        IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_GECKO)));
        igpServiceLocationCachePopulator =
                new IgpServiceLocationCachePopulator(discoveryClient, gateway);
    }

    @Test
    public void givenMultiTenancy_whenGetIgpServiceCodes_returnsIgpServiceCodes() {
        multiTenancySetup();
        Map<String, String> expected = new HashMap<>();
        expected.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
        expected.put(IgpPresets.IGPCODE_GECKO, IgpPresets.IGPCODE_IGUANA);
        expected.put(IgpPresets.IGPCODE_CHAMELEON, IgpPresets.IGPCODE_IGUANA);
        Map<String, String> igpServiceCodes = igpServiceLocationCachePopulator.getIgpServiceCodes();
        assertThat(igpServiceCodes).isEqualTo(expected);
        Mockito.verify(discoveryClient).getServices();
        Mockito.verify(gateway).getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA);
    }

    @Test
    public void givenInvalidMultiTenancy_whenGetIgpServiceCodes_throwsInvalidStateException() {
        invalidMultiTenancySetup();
        assertThatThrownBy(() -> igpServiceLocationCachePopulator.getIgpServiceCodes())
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("multiple igp match for igpCode=" + IgpPresets.IGPCODE_IGUANA);
        Mockito.verify(discoveryClient).getServices();
        Mockito.verify(gateway).getSupportedIgpCodes(IgpPresets.IGPCODE_IGUANA);
    }

    @Test
    public void givenSupportedIgpCodesNotFound_whenGetIgpServiceCodes_ReturnsEmptyMap() {
        supportedIgpCodesNotFound();
        Map<String, String> igpServiceCodes = igpServiceLocationCachePopulator.getIgpServiceCodes();
        assertThat(igpServiceCodes.isEmpty()).isTrue();
    }

    @Test
    public void givenSupportedIgpCodesNotFoundForOneOfIgpNodes_whenGetIgpServiceCodes_ReturnsMapIgpCodes() {
        Map<String, String> expected = new HashMap<>();
        expected.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
        expected.put(IgpPresets.IGPCODE_CHAMELEON, IgpPresets.IGPCODE_CHAMELEON);

        supportedIgpCodesNotFoundForOneOfIgpNodes();
        Map<String, String> igpServiceCodes = igpServiceLocationCachePopulator.getIgpServiceCodes();
        assertThat(igpServiceCodes).isEqualTo(expected);
    }
}
