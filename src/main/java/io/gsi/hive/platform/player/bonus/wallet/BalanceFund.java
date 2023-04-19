package io.gsi.hive.platform.player.bonus.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown=true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BalanceFund extends Fund {

    @NotNull
    private BigDecimal balance;

    public BalanceFund(final FundType type, final BigDecimal balance) {
        super(type);
        this.balance = balance;
    }
}
