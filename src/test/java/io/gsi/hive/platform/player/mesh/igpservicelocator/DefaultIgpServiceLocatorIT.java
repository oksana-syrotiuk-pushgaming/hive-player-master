package io.gsi.hive.platform.player.mesh.igpservicelocator;

import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.presets.IgpPresets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class DefaultIgpServiceLocatorIT extends ApiITBase {

  @Autowired
  private DefaultIgpServiceLocator igpServiceLocator;

  @MockBean
  private IgpServiceLocationCache igpServiceLocationCache;

  @Before
  public void setup() {
    Mockito.when(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_IGUANA)).thenReturn(IgpPresets.IGPCODE_IGUANA);
    Mockito.when(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_GECKO)).thenReturn(IgpPresets.IGPCODE_GECKO);
    Mockito.when(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_CHAMELEON)).thenReturn(IgpPresets.IGPCODE_CHAMELEON);
    Mockito.when(igpServiceLocationCache.getServiceCode(IgpPresets.IGPCODE_INVALID)).thenThrow(new NotFoundException("service location for igp code not found"));
  }

  @Test
  public void givenCacheWithMultipleEndpoints_whenGetServiceCode_returnCorrectServiceCode() {
    assertThat(igpServiceLocator.getServiceCode(IgpPresets.IGPCODE_IGUANA)).isEqualTo(IgpPresets.IGPCODE_IGUANA);
    assertThat(igpServiceLocator.getServiceCode(IgpPresets.IGPCODE_GECKO)).isEqualTo(IgpPresets.IGPCODE_GECKO);
    assertThat(igpServiceLocator.getServiceCode(IgpPresets.IGPCODE_CHAMELEON)).isEqualTo(IgpPresets.IGPCODE_CHAMELEON);
  }

  @Test
  public void givenCacheWithMultipleEndpoints_whenGetInvalidEndpoint_throwsNotFoundException() {
    assertThatThrownBy(() -> igpServiceLocator.getServiceCode(IgpPresets.IGPCODE_INVALID)).isInstanceOf(
            NotFoundException.class).hasMessage("service location for igp code not found");
  }
}
