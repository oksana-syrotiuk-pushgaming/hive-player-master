package io.gsi.hive.platform.player.txn.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class OperatorBonusFundDetails extends BonusFundDetails {

    public static final String TYPE = "OPERATOR";

    private String bonusId;
    private String awardId;
    private Integer remainingSpins;
    private Map<String, String> extraInfo;

    public OperatorBonusFundDetails() {
        super(TYPE);
    }

    @JsonCreator
    public OperatorBonusFundDetails(@JsonProperty("bonusId") final String bonusId,
                                    @JsonProperty("awardId") final String awardId,
                                    @JsonProperty("remainingSpins") final Integer remainingSpins,
                                    @JsonProperty("extraInfo") final Map<String, String> extraInfo) {
        this();
        this.bonusId = bonusId;
        this.awardId = awardId;
        this.remainingSpins = remainingSpins;
        this.extraInfo = extraInfo;
    }
}
