package io.gsi.hive.platform.player.mesh.txn;

import io.gsi.hive.platform.player.mesh.presets.MeshIgpPlayIdPresets;
import io.gsi.hive.platform.player.mesh.presets.MeshIgpTxnIdPresets;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerToken;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerTokenBuilder;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;
import io.gsi.hive.platform.player.mesh.wallet.MeshWalletBuilder;

import java.time.Instant;
import java.util.Date;

public class MeshGameTxnStatusBuilder {

	private String igpTxnId;
	private String igpPlayId;
	private MeshGameTxnStatus.Status status;
	private Date txnTs;
	private MeshWallet wallet;
	private MeshPlayerToken token;

	public MeshGameTxnStatusBuilder() {
		igpTxnId = MeshIgpTxnIdPresets.DEFAULT;
		igpPlayId = MeshIgpPlayIdPresets.DEFAULT;
		status = MeshGameTxnStatus.Status.OK;
		txnTs = Date.from(Instant.EPOCH);
		wallet = new MeshWalletBuilder().get();
		token = new MeshPlayerTokenBuilder().get();
	}

	public MeshGameTxnStatusBuilder withIgpTxnId(String igpTxnId) {
		this.igpTxnId = igpTxnId;
		return this;
	}

	public MeshGameTxnStatusBuilder withIgpPlayId(String igpPlayId) {
		this.igpPlayId = igpPlayId;
		return this;
	}

	public MeshGameTxnStatusBuilder withStatus(MeshGameTxnStatus.Status status) {
		this.status = status;
		return this;
	}

	public MeshGameTxnStatusBuilder withTxnTs(Date txnTs) {
		this.txnTs = txnTs;
		return this;
	}

	public MeshGameTxnStatusBuilder withWallet(MeshWallet wallet) {
		this.wallet = wallet;
		return this;
	}

	public MeshGameTxnStatusBuilder withToken(MeshPlayerToken token) {
		this.token = token;
		return this;
	}

	public MeshGameTxnStatus get() {
		return new MeshGameTxnStatus(igpTxnId, igpPlayId, status, txnTs, wallet, token);
	}
}
