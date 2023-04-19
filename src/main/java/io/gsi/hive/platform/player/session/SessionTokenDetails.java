package io.gsi.hive.platform.player.session;

import lombok.Getter;

@Getter
public class SessionTokenDetails {
    private final String sessionToken;

    public SessionTokenDetails(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}