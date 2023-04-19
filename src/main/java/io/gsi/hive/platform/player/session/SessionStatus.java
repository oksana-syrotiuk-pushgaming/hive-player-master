/*
  © gsi.io 2016
 */
package io.gsi.hive.platform.player.session;

/**
 * Session status used to block concurrent sessions being opened.
 */
public enum SessionStatus {
	ACTIVE,
	EXPIRED,
	CLOSED,
	FINISHED;
}
