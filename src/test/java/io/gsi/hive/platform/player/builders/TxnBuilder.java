package io.gsi.hive.platform.player.builders;

import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.event.TxnEvent;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;

/**
 * Player as in Hive-Player, the main driving txn
 * May be worth renaming to TxnBuilder to avoid confusing with the old PlayerTxns in Mesh
 * Better naming symmetry this way though
 * */
public class TxnBuilder {

	private String txnId = TxnPresets.TXNID;
	private String gameCode = GamePresets.CODE;
	private String playId = TxnPresets.PLAYID;
	private Boolean playComplete = true;
	private Boolean playCompleteIfCancelled = true;
	private String roundId = TxnPresets.ROUNDID;
	private Boolean roundComplete = true;
	private Boolean roundCompleteIfCancelled = true;
	private String playerId = PlayerPresets.PLAYERID;
	private String igpCode = IgpPresets.IGPCODE_IGUANA;
	private String accessToken = AuthorizationPresets.ACCESSTOKEN;
	private String sessionId = SessionPresets.SESSIONID;
	private Mode mode = Mode.real;
	private String ccyCode = MonetaryPresets.CCYCODE;
	private TxnType type = TxnType.STAKE;
	private BigDecimal amount = MonetaryPresets.BDAMOUNT;
	private BigDecimal jackpotAmount = MonetaryPresets.BDHALFAMOUNT;
	private ZonedDateTime txnTs = TimePresets.ZONEDEPOCHUTC;
	private ZonedDateTime cancelTs = null;
	private String txnRef = TxnPresets.TXNREF;
	private TxnStatus status = TxnStatus.PENDING;
	private BigDecimal balance = WalletPresets.BDBALANCE;
	private String exception = null;
	private Boolean guest = false;
	private Integer retry = 0;
	private List<TxnEvent> txnEvents;
	private Map<String, Object> extraInfo;
	private String playRef;

	private TxnBuilder()
	{
		txnEvents = new ArrayList<>();
		txnEvents.add(defaultStakeTxnRequestBuilder().build());
	}
	
	public static TxnBuilder txn(){
		return new TxnBuilder();
	}

	public TxnBuilder withTxnId(String txnId) {
		this.txnId=txnId;
		return this;
	}

	public TxnBuilder withGameCode(String gameCode)
	{
		this.gameCode = gameCode;
		return this;
	}
	public TxnBuilder withPlayId(String playId) {
		this.playId=playId;
		return this;
	}
	public TxnBuilder withPlayComplete(Boolean playComplete) {
		this.playComplete=playComplete;
		return this;
	}
	public TxnBuilder withPlayCompleteIfCancelled(Boolean playCompleteIfCancelled) {
		this.playCompleteIfCancelled=playCompleteIfCancelled;
		return this;
	}
	public TxnBuilder withRoundId(String roundId) {
		this.roundId=roundId;
		return this;
	}
	public TxnBuilder withRoundComplete(Boolean roundComplete) {
		this.roundComplete=roundComplete;
		return this;
	}
	public TxnBuilder withRoundCompleteIfCancelled(Boolean roundCompleteIfCancelled) {
		this.roundCompleteIfCancelled=roundCompleteIfCancelled;
		return this;
	}
	public TxnBuilder withPlayerId(String playerId) {
		this.playerId=playerId;
		return this;
	}
	public TxnBuilder withIgpCode(String igpCode) {
		this.igpCode=igpCode;
		return this;
	}
	public TxnBuilder withAccessToken(String accessToken) {
		this.accessToken=accessToken;
		return this;
	}
	public TxnBuilder withSessionId(String sessionId) {
		this.sessionId=sessionId;
		return this;
	}
	public TxnBuilder withMode(Mode mode) {
		this.mode=mode;
		return this;
	}
	public TxnBuilder withCcyCode(String ccyCode) {
		this.ccyCode=ccyCode;
		return this;
	}
	public TxnBuilder withType(TxnType type) {
		this.type=type;
		return this;
	}
	public TxnBuilder withAmount(BigDecimal amount) {
		this.amount=amount;
		return this;
	}
	public TxnBuilder withJackpotAmount(BigDecimal jackpotAmount) {
		this.jackpotAmount=jackpotAmount;
		return this;
	}
	public TxnBuilder withTxnTs(ZonedDateTime txnTs) {
		this.txnTs=txnTs;
		return this;
	}
	public TxnBuilder withCancelTs(ZonedDateTime cancelTs) {
		this.cancelTs=cancelTs;
		return this;
	}
	public TxnBuilder withTxnRef(String txnRef) {
		this.txnRef=txnRef;
		return this;
	}
	public TxnBuilder withStatus(TxnStatus status) {
		this.status=status;
		return this;
	}
	public TxnBuilder withBalance(BigDecimal balance) {
		this.balance=balance;
		return this;
	}
	public TxnBuilder withException(String exception) {
		this.exception=exception;
		return this;
	}
	public TxnBuilder withRetry(Integer retry) {
		this.retry=retry;
		return this;
	}
	public TxnBuilder withGuest(Boolean guest) {
		this.guest = guest;
		return this;
	}
	public TxnBuilder withTxnEvents(List<TxnEvent> events) {
		this.txnEvents = events;
		return this;
	}
	public TxnBuilder withExtraInfo(Map<String, Object> extraInfo) {
		this.extraInfo = extraInfo;
		return this;
	}

	public TxnBuilder withPlayRef(String playRef) {
		this.playRef = playRef;
		return this;
	}
	
	public Txn build(){
		Txn txn = new Txn();

		txn.setAccessToken(accessToken);
		txn.setSessionId(sessionId);
		txn.setAmount(amount);
		txn.setBalance(balance);
		txn.setCancelTs(cancelTs);
		txn.setCcyCode(ccyCode);
		txn.setException(exception);
		txn.setGameCode(gameCode);
		txn.setIgpCode(igpCode);
		txn.setJackpotAmount(jackpotAmount);
		txn.setMode(mode);
		txn.setGuest(guest);
		txn.setPlayComplete(playComplete);
		txn.setPlayCompleteIfCancelled(playCompleteIfCancelled);
		txn.setPlayerId(playerId);
		txn.setPlayId(playId);
		txn.setRetry(retry);
		txn.setRoundComplete(roundComplete);
		txn.setRoundCompleteIfCancelled(roundCompleteIfCancelled);
		txn.setRoundId(roundId);
		txn.setStatus(status);
		txn.setTxnId(txnId);
		txn.setTxnRef(txnRef);
		txn.setTxnTs(txnTs);
		txn.setType(type);
		txn.setEvents(txnEvents);
		txn.setExtraInfo(extraInfo);
		txn.setPlayRef(playRef);

		return txn;
	}
}

