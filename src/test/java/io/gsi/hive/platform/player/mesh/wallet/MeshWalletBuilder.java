package io.gsi.hive.platform.player.mesh.wallet;

import io.gsi.hive.platform.player.mesh.presets.MeshWalletPresets;
import io.gsi.hive.platform.player.wallet.Message;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class MeshWalletBuilder {

	private MeshWalletType type;
	private String currency = MeshWalletPresets.CURRENCY;
	private BigDecimal balance;
	private List<MeshWalletFund> funds;
	private Message message;

	public MeshWalletBuilder() {
		this.type = MeshWalletType.ACCOUNT;
		this.balance = MeshWalletPresets.DEFAULT_BALANCE;
		
		//Set up funds with 995 cash, 5 bonus
		this.funds = Arrays.asList(
				MeshWalletFundPresets
						.getMeshWalletBalanceFund(MeshWalletFundType.CASH, MeshWalletPresets.DEFAULT_CASH_FUND),
				MeshWalletFundPresets
						.getMeshWalletBalanceFund(MeshWalletFundType.BONUS, MeshWalletPresets.DEFAULT_BONUS_FUND)

		);
	}

	public MeshWalletBuilder withType(MeshWalletType type) {
		this.type = type;
		return this;
	}

	public MeshWalletBuilder withCurrency(String currency) {
		this.currency = currency;
		return this;
	}

	public MeshWalletBuilder withBalance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public MeshWalletBuilder withFunds(MeshWalletFund... funds) {
		this.funds = Arrays.asList(funds);
		return this;
	}

	public MeshWalletBuilder withMessage(Message message) {
		this.message = message;
		return this;
	}

	public MeshWallet get() {
		return new MeshWallet(type, currency, balance, funds, message);
	}
}
