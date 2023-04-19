package io.gsi.hive.platform.player.play.active;

import io.gsi.hive.platform.player.play.Play;
import java.util.stream.Stream;

public interface ActivePlayService {
    Stream<Play> getActivePlays();
}
