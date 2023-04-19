/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.bonus.wallet.OperatorFreeroundsFund;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * A monetary fund.  Wallets are made up of funds of various types.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "type",
		visible = true
)
@JsonSubTypes({
		@JsonSubTypes.Type(name = "FREEROUNDS", value = FreeroundsFund.class),
		@JsonSubTypes.Type(name = "CASH", value = BalanceFund.class),
		@JsonSubTypes.Type(name = "BONUS", value = BalanceFund.class),
		@JsonSubTypes.Type(name = "OPERATOR_FREEROUNDS", value = OperatorFreeroundsFund.class)
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Fund {

	@NotNull
	private FundType type;
}
