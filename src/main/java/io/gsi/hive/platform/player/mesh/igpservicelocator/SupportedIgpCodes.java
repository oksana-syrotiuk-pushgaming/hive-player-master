package io.gsi.hive.platform.player.mesh.igpservicelocator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupportedIgpCodes {
  private List<String> igpCodes;
}
