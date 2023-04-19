package io.gsi.hive.platform.player.txn;

import io.gsi.hive.platform.player.bonus.wallet.OperatorFreeroundsFund;
import io.gsi.hive.platform.player.bonus.wallet.OperatorFreeroundsFund.OperatorFreeroundsFundBuilder;
import io.gsi.hive.platform.player.presets.WalletPresets;

public class OperatorFreeroundsFundPresets {

    public static OperatorFreeroundsFundBuilder baseOperatorFreeroundsFund() {
        return OperatorFreeroundsFund.builder()
                .bonusId(WalletPresets.BONUS_ID)
                .awardId(WalletPresets.AWARD_ID)
                .extraInfo(WalletPresets.EXTRA_INFO);
    }
}
