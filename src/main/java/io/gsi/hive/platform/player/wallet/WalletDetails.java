package io.gsi.hive.platform.player.wallet;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class WalletDetails {
    private BigDecimal balance;
    private String ccyCode;
    private Message message;

    private List<Fund> funds;

    public WalletDetails(GameplayWallet gameplayWallet) {
        this.balance = gameplayWallet.getBalance();
        this.ccyCode = gameplayWallet.getCcyCode();
        this.message = gameplayWallet.getMessage();
        this.funds = gameplayWallet.getFunds();
    }
}
