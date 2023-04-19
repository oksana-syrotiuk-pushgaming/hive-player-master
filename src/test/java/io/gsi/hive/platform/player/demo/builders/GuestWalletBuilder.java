package io.gsi.hive.platform.player.demo.builders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.demo.wallet.GuestWallet;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;

public  class GuestWalletBuilder {
	private Long walletId = WalletPresets.WALLETID;
	private String playerId = PlayerPresets.PLAYERID;
	private String igpCode = IgpPresets.IGPCODE_IGUANA;
	private Boolean guest = true;
	private String gameCode = GamePresets.CODE;
	private String ccyCode = WalletPresets.CURRENCY;
	private ZonedDateTime created = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
	private BigDecimal balance = WalletPresets.BDBALANCE;
	private List<Fund> funds = new ArrayList<>();
	
	public static GuestWalletBuilder guestWallet(){
		return new GuestWalletBuilder();
	}
	
	private GuestWalletBuilder() {
		super();
		
		Fund cashFund = new BalanceFund(FundType.CASH, WalletPresets.BDBALANCE);
		funds.add(cashFund);
	}
	
	public GuestWalletBuilder withWalletId(Long walletId) {
		this.walletId=walletId;
		return this;
	}
	public GuestWalletBuilder withPlayerId(String playerId) {
		this.playerId=playerId;
		return this;
	}
	public GuestWalletBuilder withIgpCode(String igpCode) {
		this.igpCode=igpCode;
		return this;
	}
	public GuestWalletBuilder withGuest(Boolean guest) {
		this.guest=guest;
		return this;
	}
	public GuestWalletBuilder withGameCode(String gameCode) {
		this.gameCode=gameCode;
		return this;
	}
	public GuestWalletBuilder withCcyCode(String ccyCode) {
		this.ccyCode=ccyCode;
		return this;
	}
	public GuestWalletBuilder withCreated(ZonedDateTime created) {
		this.created=created;
		return this;
	}
	public GuestWalletBuilder withBalance(BigDecimal balance) {
		this.balance=balance;
		return this;
	}
	
	public GuestWallet build(){
		GuestWallet guestWallet =  new GuestWallet();
		
		guestWallet.setBalance(balance);
		guestWallet.setCcyCode(ccyCode);
		guestWallet.setCreated(created);
		guestWallet.setGameCode(gameCode);
		guestWallet.setGuest(guest);
		guestWallet.setIgpCode(igpCode);
		guestWallet.setPlayerId(playerId);
		guestWallet.setWalletId(walletId);
		guestWallet.setFunds(funds);
		
		return guestWallet;
	}
}
