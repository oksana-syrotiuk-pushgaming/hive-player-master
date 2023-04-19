package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;

public final class PlayerBuilder {
    private String playerId;
    private String igpCode;
    private String ccyCode;
    private String username;
    private String alias;
    private Boolean guest;
    private String country;
    private String lang;
    private String accessToken;
    private String jurisdiction;

    private PlayerBuilder() {
        this.playerId = PlayerPresets.PLAYERID;
        this.igpCode = IgpPresets.IGPCODE_IGUANA;
        this.ccyCode = PlayerPresets.CCY_CODE;
        this.username = PlayerPresets.USERNAME;
        this.alias = PlayerPresets.ALIAS;
        this.guest = false;
        this.country = "GB";
        this.lang = PlayerPresets.LANG;
        this.jurisdiction = PlayerPresets.JURISDICTION;
        this.accessToken = PlayerPresets.ACCESS_TOKEN;
    }

    public static PlayerBuilder aPlayer() {
        return new PlayerBuilder();
    }

    public PlayerBuilder withPlayerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    public PlayerBuilder withIgpCode(String igpCode) {
        this.igpCode = igpCode;
        return this;
    }

    public PlayerBuilder withCcyCode(String ccyCode) {
        this.ccyCode = ccyCode;
        return this;
    }

    public PlayerBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public PlayerBuilder withAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public PlayerBuilder withGuest(Boolean guest) {
        this.guest = guest;
        return this;
    }

    public PlayerBuilder withCountry(String country) {
        this.country = country;
        return this;
    }

    public PlayerBuilder withLang(String lang) {
        this.lang = lang;
        return this;
    }

    public PlayerBuilder withAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public Player build() {
        Player player = new Player();
        player.setPlayerId(playerId);
        player.setIgpCode(igpCode);
        player.setCcyCode(ccyCode);
        player.setUsername(username);
        player.setAlias(alias);
        player.setGuest(guest);
        player.setCountry(country);
        player.setLang(lang);
        player.setJurisdiction(jurisdiction);
        return player;
    }
}
