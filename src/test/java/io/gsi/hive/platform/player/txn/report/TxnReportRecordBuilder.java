package io.gsi.hive.platform.player.txn.report;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.TxnReportPresets;
import io.gsi.hive.platform.player.presets.SearchPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;

import java.math.BigDecimal;

public final class TxnReportRecordBuilder
{

    private String country;
    private String gameCode;
    private String ccyCode;
    private Long numPlays;
    private Long uniquePlayers;
    private BigDecimal totalStake;
    private BigDecimal totalWin;
    private BigDecimal bonusCost;
    private Long numFreeRounds;

    private TxnReportRecordBuilder()
    {
        this.country = SearchPresets.COUNTRY;
        this.gameCode = GamePresets.CODE;
        this.ccyCode = WalletPresets.CURRENCY;
        this.numPlays = TxnReportPresets.AGGREGATED_PLAYS;
        this.uniquePlayers = TxnReportPresets.AGGREGATED_UNIQUE_PLAYERS;
        this.totalStake = TxnReportPresets.TOTAL_STAKE;
        this.totalWin = TxnReportPresets.TOTAL_WIN;
        this.bonusCost = TxnReportPresets.BONUS_COST;
        this.numFreeRounds = TxnReportPresets.NUMBER_OF_FREEROUNDS;
    }

    public static TxnReportRecordBuilder aTxnReportRecord()
    {
        return new TxnReportRecordBuilder();
    }

    public TxnReportRecordBuilder withCountry(String country)
    {
        this.country = country;
        return this;
    }

    public TxnReportRecordBuilder withGameCode(String gameCode)
    {
        this.gameCode = gameCode;
        return this;
    }

    public TxnReportRecordBuilder withCcyCode(String ccyCode)
    {
        this.ccyCode = ccyCode;
        return this;
    }

    public TxnReportRecordBuilder withNumPlays(Long numPlays)
    {
        this.numPlays = numPlays;
        return this;
    }

    public TxnReportRecordBuilder withUniquePlayers(Long uniquePlayers)
    {
        this.uniquePlayers = uniquePlayers;
        return this;
    }

    public TxnReportRecordBuilder withTotalStake(BigDecimal totalStake)
    {
        this.totalStake = totalStake;
        return this;
    }

    public TxnReportRecordBuilder withTotalWin(BigDecimal totalWin)
    {
        this.totalWin = totalWin;
        return this;
    }

    public TxnReportRecordBuilder withBonusCost(BigDecimal bonusCost)
    {
        this.bonusCost = bonusCost;
        return this;
    }

    public TxnReportRecordBuilder withNumFreeRounds(Long numFreeRounds)
    {
        this.numFreeRounds = numFreeRounds;
        return this;
    }

    public TxnReportRecord build()
    {
        TxnReportRecord txnReportRecord = new TxnReportRecord();
        txnReportRecord.setCountry(country);
        txnReportRecord.setGameCode(gameCode);
        txnReportRecord.setCcyCode(ccyCode);
        txnReportRecord.setNumPlays(numPlays);
        txnReportRecord.setUniquePlayers(uniquePlayers);
        txnReportRecord.setTotalStake(totalStake);
        txnReportRecord.setTotalWin(totalWin);
        txnReportRecord.setBonusCost(bonusCost);
        txnReportRecord.setNumFreeRounds(numFreeRounds);
        return txnReportRecord;
    }
}
