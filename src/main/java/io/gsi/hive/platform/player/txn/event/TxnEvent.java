package io.gsi.hive.platform.player.txn.event;

import io.gsi.hive.platform.player.event.Event;
import io.gsi.hive.platform.player.event.EventType;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public abstract class TxnEvent extends Event {

	{
		this.type = EventType.txnEvent;
	}
}
