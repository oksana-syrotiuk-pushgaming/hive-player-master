package io.gsi.hive.platform.player.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.gsi.commons.exception.ForbiddenException;
import io.gsi.hive.platform.player.ApiITBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties={
    "hive.game.serviceCode.starfall=humptydumpty",
    "hive.game.disabled=jamminjars-2"
})
public class GameServiceIT extends ApiITBase {

  @Autowired
  GameService gameService;

  @Test(expected = ForbiddenException.class)
  public void giveDisabledGame_thenThrowsException() {
    gameService.getGame("jamminjars-2");
  }

  @Test
  public void givenEnabledGame_thenOk() {
    final var gameCode = "bootybay-2";
    final var expected = Game.builder()
        .code(gameCode)
        .serviceCode("bootybay")
        .status(GameStatus.active)
        .build();
    final var game = gameService.getGame(gameCode);
    assertThat(game).isEqualTo(expected);
  }

  @Test
  public void givenGameStarfall_ThenServiceCode_humptydumpty() {
    final var gameCode = "starfall";
    final var expected = Game.builder()
        .code(gameCode)
        .serviceCode("humptydumpty")
        .status(GameStatus.active)
        .build();
    final var game= gameService.getGame(gameCode);
    assertThat(game).isEqualTo(expected);
  }
}