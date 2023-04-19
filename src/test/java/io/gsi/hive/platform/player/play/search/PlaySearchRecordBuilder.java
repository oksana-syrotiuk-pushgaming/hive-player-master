package io.gsi.hive.platform.player.play.search;

import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.session.Mode;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class PlaySearchRecordBuilder {

    private String playId;
    private String playerId;
    private PlayStatus status;
    private Mode mode;
    private String gameCode;
    private boolean guest;
    private String ccyCode;
    private String igpCode;
    private ZonedDateTime createdAt;
    private ZonedDateTime modifiedAt;
    private ZonedDateTime dateTo;
    private ZonedDateTime dateFrom;
    private BigDecimal stake;
    private Integer numTxns;
    private BigDecimal win;
    private String country;
    private Boolean isFreeRound;
    private String playRef;

    private PlaySearchRecordBuilder()
    {
        this.playId = "1000-1";
        this.playerId = PlayerPresets.PLAYERID;
        this.ccyCode = PlayerPresets.CCY_CODE;
        this.createdAt = TimePresets.ZONEDEPOCHUTC;
        this.modifiedAt = TimePresets.ZONEDEPOCHUTC;
        this.dateFrom = TimePresets.ZONEDEPOCHUTC;
        this.dateTo = TimePresets.ZONEDEPOCHUTC;
        this.gameCode = GamePresets.CODE;
        this.guest = false;
        this.igpCode = IgpPresets.IGPCODE_IGUANA;
        this.numTxns = 0;
        this.status = PlayStatus.ACTIVE;
        this.stake = BigDecimal.ZERO;
        this.mode = Mode.real;
        this.win = BigDecimal.ZERO;
        this.country = PlayerPresets.COUNTRY;
        this.isFreeRound = false;
        this.playRef = "igp-play-id";
    }

    public static PlaySearchRecordBuilder aPlaySearchRecord()
    {
        return new PlaySearchRecordBuilder();
    }

    public PlaySearchRecordBuilder withGuest(boolean guest) {
        this.guest = guest;
        return this;
    }

    public PlaySearchRecordBuilder withPlayId(String playId) {
        this.playId = playId;
        return this;
    }

    public PlaySearchRecordBuilder withWin(BigDecimal win) {
        this.win = win;
        return this;
    }

    public PlaySearchRecordBuilder withStake(BigDecimal stake) {
        this.stake = stake;
        return this;
    }

    public PlaySearchRecordBuilder withDateTo(ZonedDateTime dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    public PlaySearchRecordBuilder withDateFrom(ZonedDateTime dateFrom) {
        this.dateFrom = dateFrom;
        return this;
    }

    public PlaySearchRecordBuilder withPlayerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    public PlaySearchRecordBuilder withStatus (PlayStatus status) {
        this.status = status;
        return this;
    }

    public PlaySearchRecordBuilder withMode (Mode mode) {
        this.mode = mode;
        return this;
    }

    public PlaySearchRecordBuilder withGameCode (String gameCode) {
        this.gameCode = gameCode;
        return this;
    }

    public PlaySearchRecordBuilder withCcyCode (String ccyCode) {
        this.ccyCode = ccyCode;
        return this;
    }

    public PlaySearchRecordBuilder withIgpCode (String igpCode) {
        this.igpCode = igpCode;
        return this;
    }

    public PlaySearchRecordBuilder withCreatedAt (ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public PlaySearchRecordBuilder withModifiedAt (ZonedDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
        return this;
    }

    public PlaySearchRecordBuilder withNumOfTxns (Integer numOfTxns) {
        this.numTxns = numOfTxns;
        return this;
    }

    public PlaySearchRecordBuilder withPlayRef (String playRef) {
        this.playRef = playRef;
        return this;
    }

    public PlaySearchRecord build() {
        PlaySearchRecord record = new PlaySearchRecord();
        record.setPlayId(playId);
        record.setPlayerId(playerId);
        record.setStatus(status);
        record.setNumTxns(numTxns);
        record.setMode(mode);
        record.setCcyCode(ccyCode);
        record.setIgpCode(igpCode);
        record.setCreatedAt(createdAt);
        record.setGameCode(gameCode);
        record.setModifiedAt(modifiedAt);
        record.setGuest(guest);
        record.setStake(stake);
        record.setDateFrom(dateFrom);
        record.setDateTo(dateTo);
        record.setWin(win);
        record.setCountry(country);
        record.setIsFreeRound(false);
        record.setPlayRef(playRef);
        return record;
    }
}
