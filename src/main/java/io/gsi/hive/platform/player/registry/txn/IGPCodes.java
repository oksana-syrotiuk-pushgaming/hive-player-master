package io.gsi.hive.platform.player.registry.txn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IGPCodes {
    List<String> igpCodesList = new ArrayList<>();
}
