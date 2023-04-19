package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.wallet.Wallet;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class SessionCreationDetails implements SessionDetails {
    private String sessionId;
    private String gameCode;
    private String lang;
    private Player player;
    private Wallet wallet;
    private Mode mode;

    @Override
    public String toString() {
        return "SessionDetails{" +
                "sessionId='" + sessionId + '\'' +
                ", gameCode='" + gameCode + '\'' +
                ", lang='" + lang + '\'' +
                ", player=" + player +
                ", wallet=" + wallet +
                ", mode=" + mode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionCreationDetails that = (SessionCreationDetails) o;
        return Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(gameCode, that.gameCode) &&
                Objects.equals(lang, that.lang) &&
                Objects.equals(player, that.player) &&
                Objects.equals(wallet, that.wallet) &&
                mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, gameCode, lang, player, wallet, mode);
    }
}
