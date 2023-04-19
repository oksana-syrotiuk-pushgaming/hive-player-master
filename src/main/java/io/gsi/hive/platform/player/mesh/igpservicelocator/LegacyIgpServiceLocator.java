package io.gsi.hive.platform.player.mesh.igpservicelocator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hive.igpService.locator", havingValue = "legacy")
public class LegacyIgpServiceLocator implements IgpServiceLocator {
  @Override
  public String getServiceCode(String igpCode) {
    return igpCode;
  }
}
