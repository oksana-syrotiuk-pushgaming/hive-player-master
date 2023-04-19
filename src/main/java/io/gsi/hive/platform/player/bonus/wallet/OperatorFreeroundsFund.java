package io.gsi.hive.platform.player.bonus.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown=true)
public class OperatorFreeroundsFund extends Fund {

    @NotNull
    private String bonusId;
    @NotNull
    private String awardId;

    private Map<String, String> extraInfo;

    public OperatorFreeroundsFund(
            final String bonusId,
            final String awardId,
            final Map<String, String> extraInfo) {
        this();
        this.bonusId = bonusId;
        this.awardId = awardId;
        this.extraInfo = extraInfo;
    }

    public OperatorFreeroundsFund() {
        super(FundType.OPERATOR_FREEROUNDS);
    }
}

