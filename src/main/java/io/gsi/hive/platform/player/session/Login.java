package io.gsi.hive.platform.player.session;

import io.gsi.commons.validation.ValidLang;
import io.gsi.hive.platform.player.event.Event;
import io.gsi.hive.platform.player.event.EventType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Login extends Event {
	{
		type = EventType.login;
	}
}
