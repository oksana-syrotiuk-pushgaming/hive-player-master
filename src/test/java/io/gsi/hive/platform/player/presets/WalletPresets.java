package io.gsi.hive.platform.player.presets;

import io.gsi.hive.platform.player.txn.OperatorFreeroundsFundPresets;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.Message;
import io.gsi.hive.platform.player.wallet.Wallet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WalletPresets {

	public static final String CURRENCY = "GBP";
	public static final Long WALLETID = 1L;
	public static final BigDecimal BDBALANCE = new BigDecimal("1000");
	public static final Long BONUSFUNDID = 1L;
	public static final String BONUS_ID = "bonus1";
	public static final String AWARD_ID = "award1";
	public static final Integer FREEROUNDS_REMAINING = 5;
	public static final Map<String, String> EXTRA_INFO = Map.of("extraInfoKey1", "extraInfoValue1");


	public static Wallet walletWithOperatorFreeroundsFund() {
		List<Fund> funds = new ArrayList<>();
		funds.add(OperatorFreeroundsFundPresets.baseOperatorFreeroundsFund().build());

		return Wallet.builder()
				.balance(new BigDecimal("110.00"))
				.funds(funds)
				.message(new Message("content", "type", "format"))
				.build();
	}
}
