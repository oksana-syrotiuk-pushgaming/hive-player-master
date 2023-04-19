package io.gsi.hive.platform.player.demo.builders;

import io.gsi.hive.platform.player.demo.player.GuestPlayer;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;

public final class GuestPlayerBuilder {
    private String playerId;
    private String igpCode;

    private GuestPlayerBuilder() {
        this.playerId = PlayerPresets.PLAYERID;
        this.igpCode = IgpPresets.IGPCODE_IGUANA;
    }

    public static GuestPlayerBuilder aPlayer() {
        return new GuestPlayerBuilder();
    }

    public GuestPlayerBuilder withPlayerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    public GuestPlayerBuilder withIgpCode(String igpCode) {
        this.igpCode = igpCode;
        return this;
    }

  
    public GuestPlayer build() {
        GuestPlayer player = new GuestPlayer();
        player.setPlayerId(playerId);
        player.setIgpCode(igpCode);
        return player;
    }
}
