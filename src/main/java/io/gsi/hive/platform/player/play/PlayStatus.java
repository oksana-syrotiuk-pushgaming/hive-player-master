package io.gsi.hive.platform.player.play;

import java.util.Arrays;

public enum PlayStatus {
    ACTIVE,
    FINISHED,
    VOIDED;

    public static PlayStatus findByName(String statusName)
    {
        return Arrays.stream(values()).filter(s -> s.name().equals(statusName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("status does not match"));
    }
}
