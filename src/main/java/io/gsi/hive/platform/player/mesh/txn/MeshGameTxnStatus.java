/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.txn;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.gsi.hive.platform.player.mesh.player.MeshPlayerToken;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

/**
 * The status of a processed transaction
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshGameTxnStatus {

	public enum Status {
		OK,
		CANCELLED,
		FAILED,
		TOMBSTONED
	}

	@JsonInclude(Include.NON_NULL)
	private String igpTxnId;
	@JsonInclude(Include.NON_NULL)
	private String igpPlayId;
	@NotNull
	private Status status;
	@JsonInclude(Include.NON_NULL) @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd hh:mm:ss")
	private Date txnTs;
	@JsonInclude(Include.NON_NULL) @NotNull
	private MeshWallet wallet;
	@JsonInclude(Include.NON_NULL)
	private MeshPlayerToken token;

	public MeshGameTxnStatus() {}

	@JsonCreator
	public MeshGameTxnStatus(
			@JsonProperty("igpTxnId") String igpTxnId,
			@JsonProperty("igpPlayId") String igpPlayId,
			@JsonProperty("status") Status status,
			@JsonProperty("txnTs") Date txnTs,
			@JsonProperty("wallet") MeshWallet wallet,
			@JsonProperty("token") MeshPlayerToken token) {
		this.igpTxnId = igpTxnId;
		this.igpPlayId = igpPlayId;
		this.status = status;
		this.txnTs = txnTs;
		this.wallet = wallet;
		this.token = token;
	}

	public String getIgpTxnId() {
		return igpTxnId;
	}

	public void setIgpTxnId(String igpTxnId) {
		this.igpTxnId = igpTxnId;
	}

	public String getIgpPlayId() {
		return igpPlayId;
	}

	public void setIgpPlayId(String igpPlayId) {
		this.igpPlayId = igpPlayId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Date getTxnTs() {
		return txnTs;
	}

	public void setTxnTs(Date txnTs) {
		this.txnTs = txnTs;
	}

	public MeshWallet getWallet() {
		return wallet;
	}

	public void setWallet(MeshWallet wallet) {
		this.wallet = wallet;
	}

	public MeshPlayerToken getToken() {
		return token;
	}

	public void setToken(MeshPlayerToken token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return "GameTxnStatus [igpTxnId=" + igpTxnId
				+ ", igpPlayId=" + igpPlayId
				+ ", status=" + status
				+ ", txnTs=" + txnTs
				+ ", wallet=" + wallet
				+ ", token=" + token
				+ "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MeshGameTxnStatus that = (MeshGameTxnStatus) o;
		return Objects.equals(igpTxnId, that.igpTxnId) &&
				Objects.equals(igpPlayId, that.igpPlayId) &&
				status == that.status &&
				Objects.equals(txnTs, that.txnTs) &&
				Objects.equals(wallet, that.wallet) &&
				Objects.equals(token, that.token);
	}

	@Override
	public int hashCode() {
		return Objects.hash(igpTxnId, igpPlayId, status, txnTs, wallet, token);
	}
}
