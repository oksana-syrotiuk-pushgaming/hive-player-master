package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.presets.AuthorizationPresets;
import io.gsi.hive.platform.player.wallet.Wallet;

public final class PlayerWrapperBuilder {
    private Player player;
    private Wallet wallet;
    private String authToken;

    private PlayerWrapperBuilder() {
    	this.player = PlayerBuilder.aPlayer().build();
    	this.wallet = WalletBuilder.aWallet().build();
    	this.authToken = AuthorizationPresets.ACCESSTOKEN;
    }

    public static PlayerWrapperBuilder aPlayerWrapper() {
        return new PlayerWrapperBuilder();
    }

    public PlayerWrapperBuilder withPlayer(Player player) {
        this.player = player;
        return this;
    }

    public PlayerWrapperBuilder withWallet(Wallet wallet) {
        this.wallet = wallet;
        return this;
    }

    public PlayerWrapperBuilder withAuthToken(String authToken) {
        this.authToken = authToken;
        return this;
    }

    public PlayerWrapper build() {
        PlayerWrapper playerWrapper = new PlayerWrapper();
        playerWrapper.setPlayer(player);
        playerWrapper.setWallet(wallet);
        playerWrapper.setAuthToken(authToken);
        return playerWrapper;
    }
}
