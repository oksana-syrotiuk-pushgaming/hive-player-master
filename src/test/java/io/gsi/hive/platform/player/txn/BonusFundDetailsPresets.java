package io.gsi.hive.platform.player.txn;

import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.txn.event.HiveBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.HiveBonusFundDetails.HiveBonusFundDetailsBuilder;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails.OperatorBonusFundDetailsBuilder;

public class BonusFundDetailsPresets {

	public static HiveBonusFundDetailsBuilder defaultHiveBonusFundDetails(){
		return HiveBonusFundDetails.builder()
				.fundId(WalletPresets.BONUSFUNDID);
	}

	public static OperatorBonusFundDetailsBuilder defaultOperatorBonusFundDetails() {
		return OperatorBonusFundDetails.builder()
				.bonusId(WalletPresets.BONUS_ID)
				.awardId(WalletPresets.AWARD_ID)
				.remainingSpins(WalletPresets.FREEROUNDS_REMAINING)
				.extraInfo(WalletPresets.EXTRA_INFO);
	}
}
