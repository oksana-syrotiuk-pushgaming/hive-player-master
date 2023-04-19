package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.txn.OperatorFreeroundsFundPresets;
import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.bonus.wallet.OperatorFreeroundsFund;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import io.gsi.hive.platform.player.wallet.Message;
import io.gsi.hive.platform.player.wallet.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WalletBuilder {
	private BigDecimal balance;
	private List<Fund> funds;
	private Message message;

	private WalletBuilder() {
		this.balance = WalletPresets.BDBALANCE;
		List<Fund> fundsList =  new ArrayList<>();
		fundsList.add(new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE));
		this.funds = fundsList;
	}

	public static WalletBuilder aWallet() {
		return new WalletBuilder();
	}

	//Setup presets for BonusWallet
	public static WalletBuilder freeroundsWallet() {
		FreeroundsFund fund = FreeroundsFundBuilder.freeroundsFund()
				.build();
		return new WalletBuilder()
				.withFunds(Collections.singletonList(fund))
				.withBalance(BigDecimal.ZERO);
	}

	public static WalletBuilder operatorFreeroundsWallet() {
		OperatorFreeroundsFund operatorFreeroundsFund = OperatorFreeroundsFundPresets.baseOperatorFreeroundsFund().build();
		return new WalletBuilder()
				.withFunds(Collections.singletonList(operatorFreeroundsFund))
				.withBalance(BigDecimal.ZERO);
	}

	public WalletBuilder withBalance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public WalletBuilder withFunds(List<Fund> funds) {
		this.funds = funds;
		return this;
	}

	public WalletBuilder withMessage(Message message) {
		this.message = message;
		return this;
	}

	public Wallet build() {
		Wallet wallet = new Wallet();
		wallet.setBalance(balance);
		wallet.setFunds(funds);
		wallet.setMessage(message);
		return wallet;
	}
}
