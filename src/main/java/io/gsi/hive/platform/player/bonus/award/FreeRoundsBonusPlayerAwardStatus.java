package io.gsi.hive.platform.player.bonus.award;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeRoundsBonusPlayerAwardStatus {

  public enum Status {
    active, complete, expired, forfeited, unknown;
  }

  @NotNull
  private Status status;
}
