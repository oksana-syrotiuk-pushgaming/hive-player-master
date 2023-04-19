package io.gsi.hive.platform.player.mesh.igpservicelocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "hive.igpService.locator", havingValue = "default", matchIfMissing = true)
public class DefaultIgpServiceLocator implements IgpServiceLocator {

  private final IgpServiceLocationCache igpServiceLocationCache;

  @Autowired
  public DefaultIgpServiceLocator(IgpServiceLocationCache igpServiceLocationCache) {
    this.igpServiceLocationCache = igpServiceLocationCache;
  }

  @Override
  public String getServiceCode(String igpCode) {
    return igpServiceLocationCache.getServiceCode(igpCode);
  }

}
