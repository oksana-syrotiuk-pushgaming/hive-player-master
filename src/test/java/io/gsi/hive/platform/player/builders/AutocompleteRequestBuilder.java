package io.gsi.hive.platform.player.builders;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.gsi.hive.platform.player.autocompletion.AutocompleteRequest;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;

public class AutocompleteRequestBuilder {

	private String playId;
	private String gameCode;
	private Integer retries;
	private ZonedDateTime createdAt;
	private boolean guest;
	private String sessionId;
	private String exception;

	private AutocompleteRequestBuilder() {
		super();
		this.retries = 0;
		this.createdAt = Instant.EPOCH.atZone(ZoneId.of("UTC"));
		this.playId = TxnPresets.PLAYID;
		this.gameCode = GamePresets.CODE;
		this.guest = false;
		this.sessionId = SessionPresets.SESSIONID;
	}

	public AutocompleteRequestBuilder withPlayId(String playId) {
		this.playId = playId;
		return this;
	}

	public AutocompleteRequestBuilder withGameCode(String gameCode) {
		this.gameCode = gameCode;
		return this;
	}

	public AutocompleteRequestBuilder withRetries(Integer retries) {
		this.retries = retries;
		return this;
	}

	public AutocompleteRequestBuilder withCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public AutocompleteRequestBuilder withGuest(boolean guest) {
		this.guest = guest;
		return this;
	}

	public AutocompleteRequestBuilder withSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public AutocompleteRequestBuilder withException(String exception) {
		this.exception = exception;
		return this;
	}

	public static AutocompleteRequestBuilder autocompleteRequest() {
		return new AutocompleteRequestBuilder();
	}

	public AutocompleteRequest build() {
		return AutocompleteRequest.builder()
				.createdAt(createdAt)
				.exception(exception)
				.gameCode(gameCode)
				.playId(playId)
				.retries(retries)
				.guest(guest)
				.sessionId(sessionId)
				.build();


	}

}
