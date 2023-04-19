package io.gsi.hive.platform.player.play.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayReportRecord {

  private String country;
  private String gameCode;
  private String ccyCode;
  private Long numPlays;
  private Long uniquePlayers;
  private BigDecimal totalStake;
  private BigDecimal totalWin;
  private BigDecimal grossGamingRevenue;
  private String igpCode;

}
