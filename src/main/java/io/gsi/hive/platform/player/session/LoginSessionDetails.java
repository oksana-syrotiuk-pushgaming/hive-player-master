package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.wallet.WalletDetails;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginSessionDetails implements SessionDetails {
    GameplaySessionDetails session;
    WalletDetails wallet;
}
