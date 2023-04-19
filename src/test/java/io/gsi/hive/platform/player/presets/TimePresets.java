package io.gsi.hive.platform.player.presets;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public interface TimePresets {

	ZonedDateTime ZONEDEPOCHUTC = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
	ZonedDateTime EXPECTED_STAKE_TXN_DEADLINE = ZonedDateTime
			.of(1970, 1, 1, 0, 0, 30, 0, ZoneId.of("UTC"));
}
