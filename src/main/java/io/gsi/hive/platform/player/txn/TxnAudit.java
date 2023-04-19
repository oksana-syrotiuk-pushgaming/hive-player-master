/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import io.gsi.hive.platform.player.persistence.converter.UTCDateTimeAttributeConverter;

/**
 * Txn defines a game play transaction, and stores the state of the transaction when communicating the transaction
 * to the iGP wallet.
 */
@Entity(name = "t_txn_audit")
public class TxnAudit {

	@Id @Column(name="txn_id")
	protected String txnId;

	@Column(name="action_ts") @Convert(converter=UTCDateTimeAttributeConverter.class)
	protected ZonedDateTime actionTs;

	@Column(name="action") @Enumerated(EnumType.STRING)
	private TxnAuditAction action;

	public TxnAudit() {
		this.actionTs = ZonedDateTime.now(ZoneId.of("UTC"));
	}

	public TxnAudit(String txnId, TxnAuditAction action) {
		this.actionTs = ZonedDateTime.now(ZoneId.of("UTC"));
		this.txnId = txnId;
		this.action = action;
	}

	public String getTxnId() {
		return txnId;
	}

	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}

	public ZonedDateTime getActionTs() {
		return actionTs;
	}

	public void setActionTs(ZonedDateTime actionTs) {
		this.actionTs = actionTs;
	}

	public TxnAuditAction getAction() {
		return action;
	}

	public void setAction(TxnAuditAction action) {
		this.action = action;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("TxnAudit{");
		sb.append("txnId='").append(txnId).append('\'');
		sb.append(", actionTs=").append(actionTs);
		sb.append(", action=").append(action);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TxnAudit txnAudit = (TxnAudit) o;
		return Objects.equals(getTxnId(), txnAudit.getTxnId()) &&
				Objects.equals(getActionTs(), txnAudit.getActionTs()) &&
				getAction() == txnAudit.getAction();
	}

	@Override
	public int hashCode() {

		return Objects.hash(getTxnId(), getActionTs(), getAction());
	}
}
