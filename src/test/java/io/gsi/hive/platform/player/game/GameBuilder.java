package io.gsi.hive.platform.player.game;

import io.gsi.hive.platform.player.presets.GamePresets;

public final class GameBuilder
{

    private String code;
    private GameStatus status;
    private String serviceCode;

    private GameBuilder()
    {
        this.code = GamePresets.CODE;
        this.status = GameStatus.active;
        this.serviceCode = GamePresets.CODE;
    }

    public static GameBuilder aGame()
    {
        return new GameBuilder();
    }


    public GameBuilder withCode(String code)
    {
        this.code = code;
        return this;
    }

    public GameBuilder withStatus(GameStatus status)
    {
        this.status = status;
        return this;
    }

    public GameBuilder withServiceCode(String serviceCode)
    {
        this.serviceCode = serviceCode;
        return this;
    }

    public Game build()
    {
        Game game = new Game();
        game.setCode(code);
        game.setStatus(status);
        game.setServiceCode(serviceCode);
        return game;
    }
}
