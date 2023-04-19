package io.gsi.hive.platform.player.exception;

/**Enum of all internal exception types that are suitable for returning upstream*/
public enum UpstreamGameSupportedException {

	TxnNotFoundException,
	TxnFailedException,
	PlayerStatusException,
	PlayerLimitException,
	InsufficientFundsException;
}
