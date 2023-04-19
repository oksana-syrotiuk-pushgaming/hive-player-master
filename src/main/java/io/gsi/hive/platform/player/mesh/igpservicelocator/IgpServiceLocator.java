package io.gsi.hive.platform.player.mesh.igpservicelocator;

public interface IgpServiceLocator {
  /**
   * @param igpCode igpCode
   * @return service igp code for provided igpCode
   */
  String getServiceCode(String igpCode);
}
