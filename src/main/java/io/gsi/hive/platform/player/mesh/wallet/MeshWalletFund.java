/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.wallet;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(
	use=JsonTypeInfo.Id.NAME,
	include=JsonTypeInfo.As.EXISTING_PROPERTY,
	property="type",
	visible=true
)
@JsonSubTypes({
		@JsonSubTypes.Type(name = "CASH", value = MeshWalletBalanceFund.class),
		@JsonSubTypes.Type(name = "BONUS", value = MeshWalletBalanceFund.class),
		@JsonSubTypes.Type(name = "OPERATOR_FREEROUNDS", value = MeshWalletOperatorFreeroundsFund.class)
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class MeshWalletFund {

	@NotNull
	protected MeshWalletFundType type;
}
