package io.gsi.hive.platform.player.mesh.presets;

import io.gsi.hive.platform.player.wallet.Message;
import java.math.BigDecimal;

public interface MeshWalletPresets {
	BigDecimal DEFAULT_BALANCE = new BigDecimal("1000.00");
	BigDecimal DEFAULT_CASH_FUND = new BigDecimal("995.00");
	BigDecimal DEFAULT_BONUS_FUND = new BigDecimal("5.00");
	String CURRENCY = "GBP";
	String WALLET_MESSAGE_CONTENT = "ewogICAidGV4dCI6Im1lc3NhZ2UiCn0=";
	String WALLET_MESSAGE_TYPE = "base64";
	String WALLET_MESSAGE_FORMAT = "json";

	Message WALLET_MESSAGE = new Message(
			MeshWalletPresets.WALLET_MESSAGE_CONTENT,
			MeshWalletPresets.WALLET_MESSAGE_TYPE,
			MeshWalletPresets.WALLET_MESSAGE_FORMAT
	);
}
