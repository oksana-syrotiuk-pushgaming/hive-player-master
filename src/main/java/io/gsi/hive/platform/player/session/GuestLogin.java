/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.event.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GuestLogin extends SessionCreationLogin {

	{
		this.type = EventType.guestLogin;
	}

	private String authToken;
}
