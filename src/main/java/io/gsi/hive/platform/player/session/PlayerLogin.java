/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.session;

import io.gsi.hive.platform.player.event.EventType;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerLogin extends SessionCreationLogin {

	{
		type = EventType.playerLogin;
	}

	@NotBlank
	private String authToken;

	private String playerId;
	private Integer rcMins;
}
