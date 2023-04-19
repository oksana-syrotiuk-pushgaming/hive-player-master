package io.gsi.hive.platform.player.autocompletion;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.gsi.hive.platform.player.persistence.converter.UTCDateTimeAttributeConverter;
import lombok.*;

@Builder
@Data
@AllArgsConstructor
@Entity(name="t_autocomplete_request_q")
@NoArgsConstructor
public class AutocompleteRequest {
    @Id
    @NotNull
    @Column(name="play_id")
    private String playId;
    @NotNull
    @Column(name="game_code")
    private String gameCode;
    @NotNull
    @Column(name="retries")
    private Integer retries;
    @NotNull
    @Column(name="guest")
    private Boolean guest;
    @NotNull
    @Column(name="created_at") @Convert(converter= UTCDateTimeAttributeConverter.class)
    private ZonedDateTime createdAt;
    @Column(name = "exception") @JsonIgnore
    private String exception;
    @NotNull
    @Column(name="session_id")
    private String sessionId;

    public AutocompleteRequest(String playId, String gameCode, String sessionId, boolean guest) {
    	this.playId = playId;
    	this.gameCode = gameCode;
    	this.sessionId = sessionId;
    	this.retries = 0;
    	this.createdAt = ZonedDateTime.now(ZoneId.of("UTC"));
      this.guest = guest;
    }
}
