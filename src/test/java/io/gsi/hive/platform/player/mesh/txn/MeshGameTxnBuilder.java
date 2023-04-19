package io.gsi.hive.platform.player.mesh.txn;

import io.gsi.hive.platform.player.mesh.presets.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

public class MeshGameTxnBuilder {
	private String rgsTxnId = MeshRgsTxnIdPresets.DEFAULT;
	private String rgsGameId = MeshRgsGameIdPresets.DEFAULT;
	private String rgsPlayId = MeshRgsPlayIdPresets.DEFAULT;
	private String rgsRoundId = MeshRgsRoundIdPresets.DEFAULT;
	private String playerId = MeshPlayerIdPresets.DEFAULT;
	private Boolean playComplete = false;
	private Boolean roundComplete = false;
	private String currency = MeshWalletPresets.CURRENCY;
	private List<MeshGameTxnAction> actions;
	private Object extraInfo = new Object();
	private ZonedDateTime txnDeadline;


	public MeshGameTxnBuilder() {
		this.actions = Arrays.asList(new MeshGameTxnActionBuilder().get());
	}

	public MeshGameTxnBuilder withRgsTxnId(String rgsTxnId) {
		this.rgsTxnId = rgsTxnId;
		return this;
	}

	public MeshGameTxnBuilder withRgsGameId(String rgsGameId) {
		this.rgsGameId = rgsGameId;
		return this;
	}

	public MeshGameTxnBuilder withRgsPlayId(String rgsPlayId) {
		this.rgsPlayId = rgsPlayId;
		return this;
	}

	public MeshGameTxnBuilder withRgsRoundId(String rgsRoundId) {
		this.rgsRoundId = rgsRoundId;
		return this;
	}

	public MeshGameTxnBuilder withPlayerId(String playerId) {
		this.playerId = playerId;
		return this;
	}

	public MeshGameTxnBuilder withPlayComplete(Boolean playComplete) {
		this.playComplete = playComplete;
		return this;
	}

	public MeshGameTxnBuilder withRoundComplete(Boolean roundComplete) {
		this.roundComplete = roundComplete;
		return this;
	}

	public MeshGameTxnBuilder withCurrency(String currency) {
		this.currency = currency;
		return this;
	}

	public MeshGameTxnBuilder withActions(MeshGameTxnAction... actions) {
		this.actions = Arrays.asList(actions);
		return this;
	}

	public MeshGameTxnBuilder withExtraInfo(Object extraInfo) {
		this.extraInfo = extraInfo;
		return this;
	}

	public MeshGameTxnBuilder withTxnDeadline(ZonedDateTime txnDeadline) {
		this.txnDeadline = txnDeadline;
		return this;
	}

	public MeshGameTxn get() {
		return new MeshGameTxn(rgsTxnId, rgsGameId, rgsPlayId, rgsRoundId, playerId, playComplete, roundComplete, currency, actions, txnDeadline, extraInfo);
	}

}
