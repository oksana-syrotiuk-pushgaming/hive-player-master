package io.gsi.hive.platform.player.txn.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class TxnReportRecord
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
    private String igpCode;

    public TxnReportRecord() {
        totalStake = new BigDecimal("0.00");
        totalWin = new BigDecimal("0.00");
        bonusCost = new BigDecimal("0.00");
        numFreeRounds = 0L;
        uniquePlayers = 0L;
    }

    @Override
    public String toString()
    {
        return "TxnReportRecord{" +
                "country='" + country + '\'' +
                ", gameCode='" + gameCode + '\'' +
                ", ccyCode='" + ccyCode + '\'' +
                ", numPlays=" + numPlays +
                ", uniquePlayers=" + uniquePlayers +
                ", totalStake=" + totalStake +
                ", totalWin=" + totalWin +
                ", bonusCost=" + bonusCost +
                ", numFreeRounds=" + numFreeRounds +
                ", igpCode=" + igpCode +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        TxnReportRecord that = (TxnReportRecord) o;
        return Objects.equals(getCountry(), that.getCountry()) &&
                Objects.equals(getGameCode(), that.getGameCode()) &&
                Objects.equals(getCcyCode(), that.getCcyCode()) &&
                Objects.equals(getNumPlays(), that.getNumPlays()) &&
                Objects.equals(getUniquePlayers(), that.getUniquePlayers()) &&
                Objects.equals(getTotalStake(), that.getTotalStake()) &&
                Objects.equals(getTotalWin(), that.getTotalWin()) &&
                Objects.equals(getBonusCost(), that.getBonusCost()) &&
                Objects.equals(getNumFreeRounds(), that.getNumFreeRounds()) &&
                Objects.equals(getIgpCode(), that.getIgpCode());
    }

    @Override
    public int hashCode()
    {

        return Objects.hash(getCountry(), getGameCode(), getCcyCode(), getNumPlays(), getUniquePlayers(), getTotalStake(), getTotalWin(), getBonusCost(), getNumFreeRounds(),
            getIgpCode());
    }

    @JsonIgnore
    public String getIndex()
    {
        StringBuilder key = new StringBuilder();
        if(!StringUtils.isEmpty(this.getCcyCode()))
        {
            key.append(this.getCcyCode());
        }

        if(!StringUtils.isEmpty(this.getCountry()))
        {
            key.append(this.getCountry());
        }

        if(!StringUtils.isEmpty(this.getGameCode()))
        {
            key.append(this.getGameCode());
        }
        return key.toString();
    }

    public String getCountry()
    {
        return country;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public String getGameCode()
    {
        return gameCode;
    }

    public void setGameCode(String gameCode)
    {
        this.gameCode = gameCode;
    }

    public String getCcyCode()
    {
        return ccyCode;
    }

    public void setCcyCode(String ccyCode)
    {
        this.ccyCode = ccyCode;
    }

    public Long getNumPlays()
    {
        return numPlays;
    }

    public void setNumPlays(Long numPlays)
    {
        this.numPlays = numPlays;
    }

    public Long getUniquePlayers()
    {
        return uniquePlayers;
    }

    public void setUniquePlayers(Long uniquePlayers)
    {
        this.uniquePlayers = uniquePlayers;
    }

    public BigDecimal getTotalStake()
    {
        return totalStake;
    }

    public void setTotalStake(BigDecimal totalStake)
    {
        this.totalStake = totalStake;
    }

    public BigDecimal getTotalWin()
    {
        return totalWin;
    }

    public void setTotalWin(BigDecimal totalWin)
    {
        this.totalWin = totalWin;
    }

    public BigDecimal getBonusCost()
    {
        return bonusCost;
    }

    public void setBonusCost(BigDecimal bonusCost)
    {
        this.bonusCost = bonusCost;
    }

    public Long getNumFreeRounds()
    {
        return numFreeRounds;
    }

    public void setNumFreeRounds(Long numFreeRounds)
    {
        this.numFreeRounds = numFreeRounds;
    }

    public String getIgpCode() {
        return igpCode;
    }

    public void setIgpCode(String igpCode) {
        this.igpCode = igpCode;
    }
}
