/**
 * © gsi.io 2014
 */

package io.gsi.hive.platform.player.mesh.txn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Models a game transaction
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshGameTxn {

	@NotNull
	private String rgsTxnId;
	@NotNull
	private String rgsGameId;
	@NotNull
	private String rgsPlayId;
	private String rgsRoundId;
	@NotNull
	private String playerId;
	@NotNull
	private Boolean playComplete;
	private Boolean roundComplete;
	@NotNull
	@Size(min=3,max=6)
	private String currency;
	@NotNull
	@Valid
	@Size(min=1, max=1)
	private List<MeshGameTxnAction> actions;

	private ZonedDateTime txnDeadline;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Object extraInfo;
}
