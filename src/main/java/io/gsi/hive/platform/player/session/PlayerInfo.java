package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.player.PlayerWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerInfo {

    private @Valid Login login;
    private @Valid PlayerWrapper playerWrapper;

}
