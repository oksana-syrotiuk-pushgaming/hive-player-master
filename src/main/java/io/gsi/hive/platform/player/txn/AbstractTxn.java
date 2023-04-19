/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import io.gsi.hive.platform.player.persistence.converter.UTCDateTimeAttributeConverter;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.event.TxnEvent;
import io.gsi.hive.platform.player.txn.event.TxnEventsConverter;
import io.gsi.hive.platform.player.txn.info.ExtraInfoConverter;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Txn defines a game play transaction, and stores the state of the transaction when communicating the transaction
 * to the iGP wallet.
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
public abstract class AbstractTxn {

	@Id @Column(name="txn_id")
	protected String txnId;
	@Column(name="game_code")
	protected String gameCode;
	@Column(name="play_id")
	protected String playId;
	@Column(name="play_complete")
	protected Boolean playComplete;
	@Column(name="play_complete_if_cancelled")
	protected boolean playCompleteIfCancelled;
	@Column(name="round_id")
	protected String roundId;
	@Column(name="round_complete")
	protected Boolean roundComplete;
	@Column(name="round_complete_if_cancelled")
	protected boolean roundCompleteIfCancelled;
	@Column(name="player_id")
	protected String playerId;
	@Column(name="igp_code")
	protected String igpCode;
	@Column(name="access_token") @Size(max = 256)
	protected String accessToken;
	@Column(name="session_id")
	protected String sessionId;
	@Column @Enumerated(EnumType.STRING)
	protected Mode mode;
	@Column
	protected Boolean guest;
	@Column(name="ccy_code")
	protected String ccyCode;
	@Column @Enumerated(EnumType.STRING)
	protected TxnType type;
	@Column
	protected BigDecimal amount;
	@Column(name="jackpot_amount")
	protected BigDecimal jackpotAmount;
	@Column(name="txn_ts") @Convert(converter=UTCDateTimeAttributeConverter.class)
	protected ZonedDateTime txnTs;
	@Column(name="cancel_ts") @Convert(converter=UTCDateTimeAttributeConverter.class)
	protected ZonedDateTime cancelTs;
	@Column(name="txn_ref")
	protected String txnRef;
	@Column(name="play_ref")
	protected String playRef;
	@Column @Enumerated(EnumType.STRING)
	protected TxnStatus status;
	@Column
	protected BigDecimal balance; // TODO change to storing responses for audit
	@Column
	protected String exception;
	@Column
	protected Integer retry;
	@Column(name="txn_events") @Convert(converter=TxnEventsConverter.class) @NotNull
	protected List<TxnEvent> events;

	@SuppressWarnings("JpaAttributeTypeInspection")
	@Column(name = "extra_info") @Convert(converter=ExtraInfoConverter.class)
	protected Map<String, Object> extraInfo = new HashMap<>();
	
	public AbstractTxn() {
		status = TxnStatus.PENDING;
		retry = 0;
		txnTs = ZonedDateTime.now(ZoneId.of("UTC"));
		events = new ArrayList<>();
	}

	public void addEvent(TxnEvent event){
		events.add(event);
	}

	public void incrementRetry() {
		retry++;
	}

	public boolean isReconcilable() {
		var validStatus = status == TxnStatus.PENDING || status == TxnStatus.CANCELLING;
		var validType = isStake() || isWin();
		return validStatus && validType; 
	}

	public void cancel() {
		this.status = TxnStatus.CANCELLED;
		this.cancelTs = ZonedDateTime.now(ZoneId.of("UTC"));
	}

	public boolean isStake() {
		return getType() == TxnType.STAKE || getType() == TxnType.OPFRSTK;
	}

	public boolean isWin() {
		return getType() == TxnType.WIN || getType() == TxnType.OPFRWIN;
	}
}
