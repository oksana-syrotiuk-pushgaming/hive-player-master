/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.txn.event;

import javax.validation.constraints.NotNull;

import io.gsi.hive.platform.player.event.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TxnFailure extends TxnEvent {

	{
		type = EventType.txnFailure;
	}

	@NotNull
	private String error;
}
