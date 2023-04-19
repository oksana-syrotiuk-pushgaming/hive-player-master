/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.txn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Models a specific action within a game transaction. Can be either a stake, win or refund.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "STAKE", value = MeshGameTxnAction.class),
        @JsonSubTypes.Type(name = "WIN", value = MeshGameTxnAction.class),
        @JsonSubTypes.Type(name = "RGS_FREEROUND_WIN", value = MeshGameTxnAction.class),
        @JsonSubTypes.Type(name = "RGS_FREEROUND_CLEARDOWN", value = MeshGameTxnAction.class),
        @JsonSubTypes.Type(name = "OPERATOR_FREEROUND_STAKE", value = MeshOperatorFreeroundGameTxnAction.class),
        @JsonSubTypes.Type(name = "OPERATOR_FREEROUND_WIN", value = MeshOperatorFreeroundGameTxnAction.class)
})
@Data
@NoArgsConstructor
public class MeshGameTxnAction {

    @NotNull
    private MeshGameTxnActionType type;

    @NotNull
    private String rgsActionId;

    @NotNull
    @Digits(integer = 16, fraction = 2)
    @Min(value = 0)
    private BigDecimal amount;

    @Digits(integer = 16, fraction = 2)
    @Min(value = 0)
    @JsonInclude(Include.NON_NULL)
    private BigDecimal jackpotAmount;

    @JsonCreator
    public MeshGameTxnAction(
            @JsonProperty("rgsActionId") String rgsActionId,
            @JsonProperty("type") MeshGameTxnActionType type,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("jackpotAmount") BigDecimal jackpotAmount) {
        this.rgsActionId = rgsActionId;
        this.type = type;
        this.amount = amount;
        this.jackpotAmount = jackpotAmount;
    }
}
