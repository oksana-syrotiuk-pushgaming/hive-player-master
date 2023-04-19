package io.gsi.hive.platform.player.txn;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Objects;

/**
 * TxnCallback defines details of a Gameplay transaction whose state is out of sync with the 
 * game and is awaiting being sent back to update it
 * 
 * Note that this class still used the TxnCallbackKey composite as opposed to the hive-player gameId-txnId/destination composite format
 * because this is sent back to the game as is
 */

@Entity(name="t_txn_callback_q")
public class TxnCallback 
{
	@Id @Column(name="txn_id")
	private String txnId;
	@Column(name="game_code")
	private String gameCode;
	@Column(name="txn_status") @Enumerated(EnumType.STRING)
	private TxnStatus status;
	@Column @JsonIgnore
	private int retries;
	@Column @JsonIgnore
	private String exception;

	protected TxnCallback(){}
	
	public TxnCallback(String txnId, String gameCode, TxnStatus status) {
		this.txnId = txnId;
		this.status = status;
		this.gameCode = gameCode;
		this.retries = 0;
	}
	
	public String getTxnId() {
		return txnId;
	}
	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}
	public TxnStatus getStatus() {
		return status;
	}
	public void setStatus(TxnStatus status) {
		this.status = status;
	}
	public int getRetries() {
		return retries;
	}
	public void setRetries(int retries) {
		this.retries = retries;
	}
	public String getException() {
		return exception;
	}
	public void setException(String exception) {
		this.exception = exception;
	}
	public String getGameCode() {
		return gameCode;
	}
	public void setGameCode(String gameCode) {
		this.gameCode = gameCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TxnCallback that = (TxnCallback) o;
		return retries == that.retries &&
				Objects.equals(txnId, that.txnId) &&
				Objects.equals(gameCode, that.gameCode) &&
				status == that.status &&
				Objects.equals(exception, that.exception);
	}

	@Override
	public int hashCode() {
		return Objects.hash(txnId, gameCode, status, retries, exception);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("TxnCallback{");
		sb.append("txnId='").append(txnId).append('\'');
		sb.append(", gameCode='").append(gameCode).append('\'');
		sb.append(", status=").append(status);
		sb.append(", retries=").append(retries);
		sb.append(", exception='").append(exception).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
