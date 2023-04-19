package io.gsi.hive.platform.player.txn.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        property = "type",
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        defaultImpl = HiveBonusFundDetails.class)
@JsonSubTypes({
        @JsonSubTypes.Type(name = HiveBonusFundDetails.TYPE, value = HiveBonusFundDetails.class),
        @JsonSubTypes.Type(name = OperatorBonusFundDetails.TYPE, value = OperatorBonusFundDetails.class)
})
@Data
@AllArgsConstructor
public abstract class BonusFundDetails {

    private final String type;
}
