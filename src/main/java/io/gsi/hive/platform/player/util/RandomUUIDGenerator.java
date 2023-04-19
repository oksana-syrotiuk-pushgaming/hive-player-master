/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.util;

import java.util.UUID;

public class RandomUUIDGenerator implements IdGenerator {

	@Override
	public String generate() {
		return UUID.randomUUID().toString();
	}

}
