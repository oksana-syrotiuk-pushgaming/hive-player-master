/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.txn;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * Provides information about a transaction cancellation
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshGameTxnCancel {

	@JsonCreator
	public MeshGameTxnCancel(
			@JsonProperty("playerId") String playerId,
			@JsonProperty("rgsTxnCancelId") String rgsTxnCancelId,
			@JsonProperty("playComplete") Boolean playComplete,
			@JsonProperty("roundComplete") Boolean roundComplete,
			@JsonProperty("reason") String reason,
			@JsonProperty("rgsGameId") String rgsGameId,
			@JsonProperty("currency") String currency,
			@JsonProperty("rgsRoundId") String rgsRoundId,
			@JsonProperty("rgsPlayId") String rgsPlayId,
			@JsonProperty("amount") BigDecimal amount,
			@JsonProperty("extraInfo") Map<String, Object> extraInfo) {
		this.playerId=playerId;
		this.rgsTxnCancelId=rgsTxnCancelId;
		this.playComplete=playComplete;
		this.roundComplete=roundComplete;
		this.reason=reason;
		this.rgsGameId=rgsGameId;
		this.currency=currency;
		this.rgsRoundId=rgsRoundId;
		this.rgsPlayId=rgsPlayId;
		this.amount=amount;
		this.extraInfo=extraInfo;
	}

	@NotNull
	@JsonProperty("playerId")
	private String playerId;

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("rgsTxnCancelId")
	private String rgsTxnCancelId;
	@NotNull
	@JsonProperty("playComplete")
	private Boolean playComplete;
	@JsonProperty("roundComplete")
	private Boolean roundComplete;
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("reason")
	private String reason;


	@JsonProperty("rgsGameId")
	private String rgsGameId;
	@NotNull  @Size(min=3,max=6)
	@JsonProperty("currency")
	private String currency;
	@JsonProperty("rgsRoundId")
	private String rgsRoundId;
	@JsonProperty("rgsPlayId")
	private String rgsPlayId;

	@JsonProperty("amount")
	@NotNull @Digits(integer=16,fraction=2) @Min(value=0)
	private BigDecimal amount;

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("extraInfo")
	private Map<String, Object> extraInfo;
}
