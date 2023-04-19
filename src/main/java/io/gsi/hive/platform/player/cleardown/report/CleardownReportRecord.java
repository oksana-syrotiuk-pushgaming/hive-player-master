package io.gsi.hive.platform.player.cleardown.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CleardownReportRecord {

  private String country;
  private String gameCode;
  private String ccyCode;
  private Long numCleardowns;
  private Long uniquePlayers;
  private BigDecimal totalWin;
  private String igpCode;

}
