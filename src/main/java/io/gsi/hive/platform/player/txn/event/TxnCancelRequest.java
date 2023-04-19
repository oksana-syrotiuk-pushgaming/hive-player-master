package io.gsi.hive.platform.player.txn.event;

import io.gsi.hive.platform.player.event.EventType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TxnCancelRequest extends TxnEvent {

	{
		type = EventType.txnCancelRequest;
	}
	
	@NotBlank
	private String txnId;
	@NotBlank
	private String gameCode;
	@NotNull
	private Boolean playComplete;
	@NotNull
	private Boolean roundComplete;
	@NotNull
	private TxnCancelType cancelType;
}
