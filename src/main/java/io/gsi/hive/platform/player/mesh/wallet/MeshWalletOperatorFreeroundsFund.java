package io.gsi.hive.platform.player.mesh.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class MeshWalletOperatorFreeroundsFund extends MeshWalletFund {

    @NotNull
    private String bonusId;
    @NotNull
    private String awardId;

    private Map<String, String> extraInfo;

    public MeshWalletOperatorFreeroundsFund(
            final String bonusId,
            final String awardId,
            final Map<String, String> extraInfo) {
        this();
        this.bonusId = bonusId;
        this.awardId = awardId;
        this.extraInfo = extraInfo;
    }

    public MeshWalletOperatorFreeroundsFund() {
        super(MeshWalletFundType.OPERATOR_FREEROUNDS);
    }
}
