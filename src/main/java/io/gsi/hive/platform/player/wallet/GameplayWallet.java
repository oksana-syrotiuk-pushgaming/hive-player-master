/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Player's wallet
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameplayWallet {

    private BigDecimal balance;

    private String ccyCode;

    private List<Fund> funds;

    @JsonInclude(Include.NON_NULL)
    private Message message;

}
