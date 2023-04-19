/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.event;

import java.time.Instant;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.session.Login;
import io.gsi.hive.platform.player.session.PlayerLogin;
import io.gsi.hive.platform.player.session.SessionTokenLogin;
import io.gsi.hive.platform.player.txn.event.*;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(
		use=JsonTypeInfo.Id.NAME,
		include=JsonTypeInfo.As.EXISTING_PROPERTY,
		property="type",
		visible=false
		)
@JsonSubTypes({
	@JsonSubTypes.Type(name="playerLogin",value=PlayerLogin.class),
	@JsonSubTypes.Type(name="guestLogin",value=GuestLogin.class),
	@JsonSubTypes.Type(name="login",value=Login.class),
	@JsonSubTypes.Type(name="txnRequest",value=TxnRequest.class),
	@JsonSubTypes.Type(name="txnCancelRequest",value=TxnCancelRequest.class),
	@JsonSubTypes.Type(name="txnReceipt",value=TxnReceipt.class),
	@JsonSubTypes.Type(name="txnEvent",value=TxnEvent.class),
	@JsonSubTypes.Type(name="txnCleardown",value= TxnCleardownEvent.class),
	@JsonSubTypes.Type(name="sessionTokenLogin",value= SessionTokenLogin.class)
})
@Data
public abstract class Event {

	protected EventType type;

	@NotNull
	protected Long timestamp;

	public Event() {
		timestamp = Instant.now().toEpochMilli();
	}
}
