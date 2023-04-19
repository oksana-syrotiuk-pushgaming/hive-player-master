package io.gsi.hive.platform.player.mesh.txn;


import java.math.BigDecimal;

import io.gsi.hive.platform.player.presets.MonetaryPresets;

public class MeshGameTxnActionBuilder {

	private String rgsActionId;
	private MeshGameTxnActionType type;
	private BigDecimal amount;
	private BigDecimal jackpotAmount;

	public MeshGameTxnActionBuilder() {
		rgsActionId = "1";
		type = MeshGameTxnActionType.STAKE;
		amount =MonetaryPresets.BDAMOUNT;
	}

	public MeshGameTxnActionBuilder withRgsActionId(String rgsActionId) {
		this.rgsActionId = rgsActionId;
		return this;
	}

	public MeshGameTxnActionBuilder withType(MeshGameTxnActionType type) {
		this.type = type;
		return this;
	}

	public MeshGameTxnActionBuilder withAmount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public MeshGameTxnActionBuilder withJackpotAmount(BigDecimal jackpotAmount) {
		this.jackpotAmount = jackpotAmount;
		return this;
	}

	public MeshGameTxnAction get() {
		return new MeshGameTxnAction(rgsActionId,type,amount, jackpotAmount);
	}
}
