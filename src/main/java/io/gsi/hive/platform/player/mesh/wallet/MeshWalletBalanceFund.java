package io.gsi.hive.platform.player.mesh.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MeshWalletBalanceFund extends MeshWalletFund {

    @NotNull
    @Digits(integer = 12, fraction = 2)
    @Min(value = 0)
    protected BigDecimal balance;

    public MeshWalletBalanceFund(final MeshWalletFundType meshWalletFundType, final BigDecimal balance) {
        super(meshWalletFundType);
        this.balance = balance;
    }
}
