package io.gsi.hive.platform.player.mesh.igpservicelocator;

import io.gsi.hive.platform.player.presets.IgpPresets;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegacyIgpServiceLocatorTests {

  private LegacyIgpServiceLocator legacyIgpServiceLocator;

  @Before
  public void setup() {
    this.legacyIgpServiceLocator = new LegacyIgpServiceLocator();
  }

  @Test
  public void givenDefaultIgpCode_whenGetServiceCode_returnsDefaultIgpCode() {
    String expected = IgpPresets.IGPCODE_IGUANA;
    String igpCode = legacyIgpServiceLocator.getServiceCode(IgpPresets.IGPCODE_IGUANA);
    assertThat(igpCode).isEqualTo(expected);
  }

  @Test
  public void givenSecondaryIgpCode_whenGetServiceCode_returnsSecondaryIgpCode() {
    String expected = IgpPresets.IGPCODE_GECKO;
    String igpCode = legacyIgpServiceLocator.getServiceCode(IgpPresets.IGPCODE_GECKO);
    assertThat(igpCode).isEqualTo(expected);
  }

  @Test
  public void givenTertiaryIgpCode_whenGetServiceCode_returnsTertiaryIgpCode() {
    String expected = IgpPresets.IGPCODE_CHAMELEON;
    String igpCode = legacyIgpServiceLocator.getServiceCode(IgpPresets.IGPCODE_CHAMELEON);
    assertThat(igpCode).isEqualTo(expected);
  }
}
