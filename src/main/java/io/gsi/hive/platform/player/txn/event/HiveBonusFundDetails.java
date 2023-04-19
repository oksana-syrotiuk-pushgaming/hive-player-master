package io.gsi.hive.platform.player.txn.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class HiveBonusFundDetails extends BonusFundDetails {

	public static final String TYPE = "HIVE";

	@Min(0)
	@NotNull
	private Long fundId;

	public HiveBonusFundDetails() {
		super(TYPE);
	}

	@JsonCreator
	public HiveBonusFundDetails(@JsonProperty("fundId") final Long fundId) {
		this();
		this.fundId = fundId;
	}
}
