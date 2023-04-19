package io.gsi.hive.platform.player.mesh.igpservicelocator;

import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.presets.IgpPresets;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class IgpServiceLocationCacheIT extends ApiITBase {

  @MockBean
  private IgpServiceLocationCachePopulator igpServiceLocationCachePopulator;

  @Autowired
  private IgpServiceLocationCache igpServiceLocationCache;

  @Before
  public void setUp() {
    ReflectionTestUtils.invokeMethod(igpServiceLocationCache,"clear", Collections.emptyMap());
  }

  @Test
  public void givenInvalidIgpCode_whenGetServiceLocation_throwsNotFoundException() {
    Map<String, String> testIgpServiceCodes = new HashMap<>();
    testIgpServiceCodes.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(testIgpServiceCodes);

    assertThatThrownBy(() -> igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_INVALID))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("service location for igpCode=invalidIgpCode not found");
  }

  @Test
  public void givenValidIgpCode_whenGetServiceCode_returnsCorrectEndpoint() {
    Map<String, String> testIgpServiceCodes = new HashMap<>();
    testIgpServiceCodes.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(testIgpServiceCodes);

    igpServiceLocationCache.populateIgpServiceCodes();
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);

    verify(igpServiceLocationCachePopulator, times(1)).getIgpServiceCodes();
  }

  @Test
  public void givenValidIgpCode_whenRepopulatingIntervalOne_returnsCorrectEndpoint() {
    Map<String, String> testIgpServiceCodes = new HashMap<>();
    testIgpServiceCodes.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(testIgpServiceCodes);

    igpServiceLocationCache.populateIgpServiceCodes();
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);

    ReflectionTestUtils.setField(igpServiceLocationCache, "currentRepopulatingInterval", new AtomicInteger(1));
    igpServiceLocationCache.populateIgpServiceCodes();
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);

    verify(igpServiceLocationCachePopulator, times(2)).getIgpServiceCodes();
  }

  @Test
  public void givenMultipleIgpCodesServedBySingleEndpoint_whenGetServiceCode_returnsCorrectEndpoints() {
    Map<String, String> testIgpServiceCodes = new HashMap<>();
    testIgpServiceCodes.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
    testIgpServiceCodes.put(IgpPresets.IGPCODE_GECKO, IgpPresets.IGPCODE_IGUANA);
    testIgpServiceCodes.put(IgpPresets.IGPCODE_CHAMELEON, IgpPresets.IGPCODE_IGUANA);
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(testIgpServiceCodes);

    igpServiceLocationCache.populateIgpServiceCodes();
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_GECKO))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_CHAMELEON))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);

    verify(igpServiceLocationCachePopulator).getIgpServiceCodes();
  }

  @Test
  public void givenMultipleIgpServiceCodes_whenGetIgpServiceCodes_returnsCorrectIgpServiceCodes() {
    Map<String, String> testIgpServiceCodes = new HashMap<>();
    testIgpServiceCodes.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
    testIgpServiceCodes.put(IgpPresets.IGPCODE_GECKO, IgpPresets.IGPCODE_GECKO);
    testIgpServiceCodes.put(IgpPresets.IGPCODE_CHAMELEON, IgpPresets.IGPCODE_CHAMELEON);
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(testIgpServiceCodes);

    igpServiceLocationCache.populateIgpServiceCodes();
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_GECKO))
            .isEqualTo(IgpPresets.IGPCODE_GECKO);
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_CHAMELEON))
            .isEqualTo(IgpPresets.IGPCODE_CHAMELEON);

    verify(igpServiceLocationCachePopulator, times(1)).getIgpServiceCodes();
  }

  @Test
  public void givenCachedIgpServiceCodes_whenPopulateAndError_thenCacheHasCurrentState()
          throws InterruptedException {
    final Map<String, String> firstFetchIgpServiceCodes = new HashMap<>();
    firstFetchIgpServiceCodes.put(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);

    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(firstFetchIgpServiceCodes);

    igpServiceLocationCache.populateIgpServiceCodes();
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);
    verify(igpServiceLocationCachePopulator, times(1)).getIgpServiceCodes();

    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenThrow(IllegalStateException.class);

    assertThatThrownBy(() -> igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_GECKO)).isInstanceOf(NotFoundException.class);
    verify(igpServiceLocationCachePopulator, times(1)).getIgpServiceCodes();

    // verify code still in cache after error
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);
    verify(igpServiceLocationCachePopulator, times(1)).getIgpServiceCodes();
  }

  @Test
  public void givenRemovedIgpServiceCodes_whenRepopulateAndClear_thenIgpServiceCodeRemoved() {
    final Map<String, String> initialIgpServiceCodes = Map.of(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
    final Map<String, String> latestIgpServiceCodes = Map.of(IgpPresets.IGPCODE_GECKO, IgpPresets.IGPCODE_GECKO);

    ReflectionTestUtils.setField(igpServiceLocationCache, "currentRepopulatingInterval", new AtomicInteger(2));
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(initialIgpServiceCodes);
    igpServiceLocationCache.populateIgpServiceCodes();
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(latestIgpServiceCodes);
    igpServiceLocationCache.populateIgpServiceCodes();

    assertThatThrownBy(() -> igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA)).isInstanceOf(NotFoundException.class);
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_GECKO))
            .isEqualTo(IgpPresets.IGPCODE_GECKO);

    verify(igpServiceLocationCachePopulator, times(2)).getIgpServiceCodes();
  }

  @Test
  public void givenRemovedIgpServiceCodes_whenRepopulateOnly_thenIgpServiceCodeNotRemoved() {
    final Map<String, String> initialIgpServiceCodes = Map.of(IgpPresets.IGPCODE_IGUANA, IgpPresets.IGPCODE_IGUANA);
    final Map<String, String> latestIgpServiceCodes = Map.of(IgpPresets.IGPCODE_GECKO, IgpPresets.IGPCODE_GECKO);

    ReflectionTestUtils.setField(igpServiceLocationCache, "currentRepopulatingInterval", new AtomicInteger(3));
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(initialIgpServiceCodes);
    igpServiceLocationCache.populateIgpServiceCodes();
    when(igpServiceLocationCachePopulator.getIgpServiceCodes()).thenReturn(latestIgpServiceCodes);
    igpServiceLocationCache.populateIgpServiceCodes();

    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA))
            .isEqualTo(IgpPresets.IGPCODE_IGUANA);
    assertThat(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_GECKO))
            .isEqualTo(IgpPresets.IGPCODE_GECKO);

    verify(igpServiceLocationCachePopulator, times(2)).getIgpServiceCodes();
  }
}
