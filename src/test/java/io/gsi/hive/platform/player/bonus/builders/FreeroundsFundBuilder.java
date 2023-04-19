package io.gsi.hive.platform.player.bonus.builders;

import java.math.BigDecimal;

import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.presets.WalletPresets;

public class FreeroundsFundBuilder {

	private Long fundId;
	private Integer awarded;
	private Integer remaining;
	private BigDecimal betAmount;
	private BigDecimal cumulativeWin;
	private String awardId;
	private String bonusId;

	public FreeroundsFundBuilder withFundId(Long fundId) {
		this.fundId=fundId;
		return this;
	}
	public FreeroundsFundBuilder withAwarded(Integer awarded) {
		this.awarded=awarded;
		return this;
	}
	public FreeroundsFundBuilder withAwardId(String awardId) {
		this.awardId=awardId;
		return this;
	}
	public FreeroundsFundBuilder withBonusId(String bonusId) {
		this.bonusId=bonusId;
		return this;
	}
	public FreeroundsFundBuilder withRemaining(Integer remaining) {
		this.remaining=remaining;
		return this;
	}
	public FreeroundsFundBuilder withBetAmount(BigDecimal betAmount) {
		this.betAmount=betAmount;
		return this;
	}
	public FreeroundsFundBuilder withCumulativeWin(BigDecimal cumulativeWin) {
		this.cumulativeWin=cumulativeWin;
		return this;
	}

	private FreeroundsFundBuilder()
	{
		this.awarded = 10;
		this.awardId="award1";
		this.bonusId="bonus1";
		this.cumulativeWin = new BigDecimal(0.00);
		this.fundId = WalletPresets.BONUSFUNDID;
		this.remaining = 10;
		this.betAmount = new BigDecimal("1.00");
	}

	public FreeroundsFund build(){
		FreeroundsFund fund = new FreeroundsFund();
		fund.setAwarded(awarded);
		fund.setAwardId(awardId);
		fund.setBonusId(bonusId);
		fund.setCumulativeWin(cumulativeWin);
		fund.setFundId(fundId);
		fund.setRemaining(remaining);
		fund.setBetAmount(betAmount);
		return fund;
	}

	public static FreeroundsFundBuilder freeroundsFund(){
		return new FreeroundsFundBuilder();
	}
}

