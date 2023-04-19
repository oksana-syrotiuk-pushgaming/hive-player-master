package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.wallet.GameplayWallet;
import io.gsi.hive.platform.player.wallet.Message;

import java.math.BigDecimal;

public class GameplayWalletBuilder {
    private BigDecimal balance;
    private String ccyCode;
    private Message message;

    private GameplayWalletBuilder() {
        this.balance = WalletPresets.BDBALANCE;
        this.ccyCode = "GBP";
    }

    public static GameplayWalletBuilder aWallet() {
        return new GameplayWalletBuilder();
    }

    public GameplayWalletBuilder withBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    public GameplayWalletBuilder withMessage(Message message) {
        this.message = message;
        return this;
    }

    public GameplayWalletBuilder withCcyCode(String ccyCode) {
        this.ccyCode = ccyCode;
        return this;
    }

    public GameplayWallet build() {
        GameplayWallet wallet = new GameplayWallet();
        wallet.setBalance(balance);
        wallet.setCcyCode(ccyCode);
        wallet.setMessage(message);
        return wallet;
    }
}
