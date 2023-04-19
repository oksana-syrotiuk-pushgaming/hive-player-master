package io.gsi.hive.platform.player.bonus.wallet;

import java.math.BigDecimal;
import java.util.Objects;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown=true)
@Data
@EqualsAndHashCode(callSuper = true)
public class FreeroundsFund extends Fund {
	
	@NotNull
	private Long fundId;
	@NotNull
	private Integer awarded;
	@NotNull
	private String awardId;
	@NotNull
	private String bonusId;
	@NotNull
	private Integer remaining;
	@NotNull @Digits(integer=12,fraction=2) @Min(value=0)
	private BigDecimal cumulativeWin;

	private BigDecimal betAmount;

	public FreeroundsFund(FreeroundsFund fund) {
		this();
		
		this.fundId = fund.fundId;
		this.awarded = fund.awarded;
		this.awardId = fund.awardId;
		this.bonusId = fund.bonusId;
		this.remaining = fund.remaining;
		this.cumulativeWin = fund.cumulativeWin;
		this.betAmount = fund.betAmount;
	}
	
	public FreeroundsFund() {
		super(FundType.FREEROUNDS);
	}
}
