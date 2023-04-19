/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.session;

import java.util.Arrays;

/**
 * Game play mode. real or demo
 */
public enum Mode {
	real,
	demo;
	public static Mode findModeByName(String modeName)
	{
		return Arrays.stream(values()).filter(m -> m.name().equalsIgnoreCase(modeName))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("bad mode specified"));
	}
}
