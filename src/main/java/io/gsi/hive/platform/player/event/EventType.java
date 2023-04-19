/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.event;

public enum EventType {
	playerLogin,
	guestLogin,
	login,
	txnEvent,
	txnRequest,
	txnReceipt,
	txnCancelRequest,
	txnFailure,
    sessionTokenLogin,
	txnCleardown
}
