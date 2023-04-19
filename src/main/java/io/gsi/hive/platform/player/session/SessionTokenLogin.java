package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.event.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SessionTokenLogin extends Login {

    {
        this.type = EventType.sessionTokenLogin;
    }
    private String sessionToken;
}
