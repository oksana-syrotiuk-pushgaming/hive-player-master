package io.gsi.hive.platform.player.api.s2s;

import io.gsi.hive.platform.player.play.search.PlaySearchArguments;

import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayService;

@RestController
@RequestMapping("/s2s/platform/player/v1")
@Loggable
public class S2SPlayController {

  private final PlayService playService;

  public S2SPlayController(PlayService playService) {
    this.playService = playService;
  }

  @PostMapping(path="/play/void")
  public Play voidPlay(@RequestParam String playId){
    return playService.voidPlay(playId);
  }

  @GetMapping(path="/play/{playId}")
  public Play getPlay(@PathVariable String playId) {
    return playService.getPlay(playId);
  }

  @GetMapping(path="/plays")
  public List<Play> getPlays(@Valid PlaySearchArguments playSearchArguments) {
    return playService.getPlays(playSearchArguments);
  }
}
