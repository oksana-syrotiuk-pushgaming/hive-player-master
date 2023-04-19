package io.gsi.hive.platform.player.txn.search;

import io.gsi.hive.platform.player.mesh.presets.MeshRgsPlayIdPresets;
import io.gsi.hive.platform.player.mesh.presets.MeshRgsRoundIdPresets;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.SearchPresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public final class TxnSearchRecordBuilder
{

    private String txnId;
    private String gameCode;
    private String playId;
    private boolean playComplete;
    private boolean playCompleteIfCancelled;
    private String roundId;
    private boolean roundComplete;
    private boolean roundCompleteIfCancelled;
    private String playerId;
    private String username;
    private String country;
    private String igpCode;
    private String sessionId;
    private Mode mode;
    private boolean guest;
    private boolean bonus;
    private String ccyCode;
    private TxnType type;
    private BigDecimal amount;
    private BigDecimal jackpotAmount;
    private ZonedDateTime txnTs;
    private ZonedDateTime cancelTs;
    private String txnRef;
    private TxnStatus status;
    private String accessToken;

    private TxnSearchRecordBuilder()
    {
        this.txnId = TxnPresets.TXNID;
        this.gameCode = GamePresets.CODE;
        this.playId = MeshRgsPlayIdPresets.DEFAULT;
        this.playComplete = MeshRgsPlayIdPresets.PLAY_COMPLETE;
        this.playCompleteIfCancelled = MeshRgsPlayIdPresets.PLAY_COMPLETE_IF_CANCELLED;
        this.roundId = MeshRgsRoundIdPresets.DEFAULT;
        this.roundComplete = MeshRgsRoundIdPresets.ROUND_COMPLETE;
        this.roundCompleteIfCancelled = MeshRgsRoundIdPresets.ROUND_COMPLETE_IF_CANCELLED;
        this.playerId = PlayerPresets.PLAYERID;
        this.username = PlayerPresets.USERNAME;
        this.country = PlayerPresets.COUNTRY;
        this.igpCode = IgpPresets.IGPCODE_IGUANA;
        this.sessionId = SessionPresets.SESSIONID;
        this.mode = SearchPresets.MODE;
        this.guest = SearchPresets.GUEST;
        this.bonus = SearchPresets.BONUS;
        this.ccyCode = WalletPresets.CURRENCY;
        this.type = TxnPresets.TYPE;
        this.amount = SearchPresets.AMOUNT;
        this.jackpotAmount = SearchPresets.JACKPOT_AMOUNT;
        this.txnTs = TimePresets.ZONEDEPOCHUTC;
        this.cancelTs = TimePresets.ZONEDEPOCHUTC;
        this.txnRef = TxnPresets.TXNREF;
        this.status = SearchPresets.STATUS;
        this.accessToken = TxnPresets.ACCESSTOKEN;

    }

    public static TxnSearchRecordBuilder aTxnSearchRecord()
    {
        return new TxnSearchRecordBuilder();
    }

    public TxnSearchRecordBuilder withTxnId(String txnId)
    {
        this.txnId = txnId;
        return this;
    }

    public TxnSearchRecordBuilder withGameCode(String gameCode)
    {
        this.gameCode = gameCode;
        return this;
    }

    public TxnSearchRecordBuilder withPlayId(String playId)
    {
        this.playId = playId;
        return this;
    }

    public TxnSearchRecordBuilder withPlayComplete(boolean playComplete)
    {
        this.playComplete = playComplete;
        return this;
    }

    public TxnSearchRecordBuilder withPlayCompleteIfCancelled(boolean playCompleteIfCancelled)
    {
        this.playCompleteIfCancelled = playCompleteIfCancelled;
        return this;
    }

    public TxnSearchRecordBuilder withRoundId(String roundId)
    {
        this.roundId = roundId;
        return this;
    }

    public TxnSearchRecordBuilder withRoundComplete(boolean roundComplete)
    {
        this.roundComplete = roundComplete;
        return this;
    }

    public TxnSearchRecordBuilder withRoundCompleteIfCancelled(boolean roundCompleteIfCancelled)
    {
        this.roundCompleteIfCancelled = roundCompleteIfCancelled;
        return this;
    }

    public TxnSearchRecordBuilder withPlayerId(String playerId)
    {
        this.playerId = playerId;
        return this;
    }

    public TxnSearchRecordBuilder withUsername(String username)
    {
        this.username = username;
        return this;
    }

    public TxnSearchRecordBuilder withCountry(String country)
    {
        this.country = country;
        return this;
    }

    public TxnSearchRecordBuilder withIgpCode(String igpCode)
    {
        this.igpCode = igpCode;
        return this;
    }

    public TxnSearchRecordBuilder withSessionId(String sessionId)
    {
        this.sessionId = sessionId;
        return this;
    }

    public TxnSearchRecordBuilder withMode(Mode mode)
    {
        this.mode = mode;
        return this;
    }

    public TxnSearchRecordBuilder withGuest(boolean guest)
    {
        this.guest = guest;
        return this;
    }

    public TxnSearchRecordBuilder withBonus(boolean bonus)
    {
        this.bonus = bonus;
        return this;
    }

    public TxnSearchRecordBuilder withCcyCode(String ccyCode)
    {
        this.ccyCode = ccyCode;
        return this;
    }

    public TxnSearchRecordBuilder withType(TxnType type)
    {
        this.type = type;
        return this;
    }

    public TxnSearchRecordBuilder withAmount(BigDecimal amount)
    {
        this.amount = amount;
        return this;
    }

    public TxnSearchRecordBuilder withJackpotAmount(BigDecimal jackpotAmount)
    {
        this.jackpotAmount = jackpotAmount;
        return this;
    }

    public TxnSearchRecordBuilder withTxnTs(ZonedDateTime txnTs)
    {
        this.txnTs = txnTs;
        return this;
    }

    public TxnSearchRecordBuilder withCancelTs(ZonedDateTime cancelTs)
    {
        this.cancelTs = cancelTs;
        return this;
    }

    public TxnSearchRecordBuilder withTxnRef(String txnRef)
    {
        this.txnRef = txnRef;
        return this;
    }

    public TxnSearchRecordBuilder withStatus(TxnStatus status)
    {
        this.status = status;
        return this;
    }

    public TxnSearchRecordBuilder withAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
        return this;
    }

    public TxnSearchRecord build()
    {
        TxnSearchRecord txnSearchRecord = new TxnSearchRecord();
        txnSearchRecord.setTxnId(txnId);
        txnSearchRecord.setGameCode(gameCode);
        txnSearchRecord.setPlayId(playId);
        txnSearchRecord.setPlayComplete(playComplete);
        txnSearchRecord.setPlayCompleteIfCancelled(playCompleteIfCancelled);
        txnSearchRecord.setRoundId(roundId);
        txnSearchRecord.setRoundComplete(roundComplete);
        txnSearchRecord.setRoundCompleteIfCancelled(roundCompleteIfCancelled);
        txnSearchRecord.setPlayerId(playerId);
        txnSearchRecord.setUsername(username);
        txnSearchRecord.setCountry(country);
        txnSearchRecord.setIgpCode(igpCode);
        txnSearchRecord.setSessionId(sessionId);
        txnSearchRecord.setMode(mode);
        txnSearchRecord.setGuest(guest);
        txnSearchRecord.setBonus(bonus);
        txnSearchRecord.setCcyCode(ccyCode);
        txnSearchRecord.setType(type);
        txnSearchRecord.setAmount(amount);
        txnSearchRecord.setJackpotAmount(jackpotAmount);
        txnSearchRecord.setTxnTs(txnTs);
        txnSearchRecord.setCancelTs(cancelTs);
        txnSearchRecord.setTxnRef(txnRef);
        txnSearchRecord.setStatus(status);
        txnSearchRecord.setAccessToken(accessToken);
        return txnSearchRecord;
    }
}
