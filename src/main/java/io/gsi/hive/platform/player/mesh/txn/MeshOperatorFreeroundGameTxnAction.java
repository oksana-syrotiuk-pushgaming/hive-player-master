package io.gsi.hive.platform.player.mesh.txn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MeshOperatorFreeroundGameTxnAction extends MeshGameTxnAction {

    @NotNull
    private String bonusId;
    @NotNull
    private String awardId;
    @NotNull
    private Integer freeroundsRemaining;

    private Map<String, String> extraInfo;

    @JsonCreator
    public MeshOperatorFreeroundGameTxnAction(
            @JsonProperty("type") MeshGameTxnActionType type,
            @JsonProperty("rgsActionId") String rgsActionId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("jackpotAmount") BigDecimal jackpotAmount,
            @JsonProperty("bonusId") String bonusId,
            @JsonProperty("awardId") String awardId,
            @JsonProperty("freeroundsRemaining") Integer freeroundsRemaining,
            @JsonProperty("extraInfo") Map<String, String> extraInfo) {
        super(rgsActionId, type, amount, jackpotAmount);
        this.bonusId = bonusId;
        this.awardId = awardId;
        this.freeroundsRemaining = freeroundsRemaining;
        this.extraInfo = extraInfo;
    }
}
