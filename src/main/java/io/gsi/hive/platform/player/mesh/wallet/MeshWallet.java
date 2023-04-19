/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.gsi.hive.platform.player.wallet.Message;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
public class MeshWallet {

    @NotNull
    private MeshWalletType type;

    @NotNull
    @Size(min = 3, max = 6)
    private String currency;

    @NotNull
    @Digits(integer = 16, fraction = 2)
    @Min(value = 0)
    private BigDecimal balance;

    @NotNull
    private List<MeshWalletFund> funds;

    @JsonInclude(Include.NON_NULL)
    private Message message;

    public MeshWallet() {
        funds = new ArrayList<>();
    }
}
