package io.gsi.hive.platform.player.mesh.wallet;

import io.gsi.hive.platform.player.mesh.presets.MeshWalletPresets;

import io.gsi.hive.platform.player.presets.WalletPresets;
import java.math.BigDecimal;

public class MeshWalletFundPresets {

	public static MeshWalletBalanceFund getMeshWalletBalanceFund() {
		return new MeshWalletBalanceFund(MeshWalletFundType.CASH, MeshWalletPresets.DEFAULT_BALANCE);
	}

	public static MeshWalletBalanceFund getMeshWalletBalanceFund(MeshWalletFundType type, BigDecimal balance) {
		return new MeshWalletBalanceFund(type, balance);
	}

	public static MeshWalletOperatorFreeroundsFund getMeshWalletOperatorFreeRoundsFund() {
		return new MeshWalletOperatorFreeroundsFund(
				WalletPresets.BONUS_ID, WalletPresets.AWARD_ID, WalletPresets.EXTRA_INFO);
	}
}
