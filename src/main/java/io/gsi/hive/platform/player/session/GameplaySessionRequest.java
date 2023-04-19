package io.gsi.hive.platform.player.session;

import io.gsi.commons.validation.ValidLang;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode
public class GameplaySessionRequest {

    @NotBlank
    private String playerId;
    private String authToken;
    private String guestToken;
    @NotBlank
    private String accessToken;
    @NotBlank
    protected String igpCode;
    protected String integrationCode;
    @NotBlank
    protected String gameCode;
    @NotNull
    protected Mode mode;
    @NotBlank
    private String ccyCode;
    @NotBlank
    private String countryCode;
    @NotBlank
    private String regionCode;
    @NotBlank
    private String jurisdiction;
    @ValidLang
    protected String lang;
    @NotNull
    private ClientType clientType;
    @NotBlank
    private String launchReferrer;
    @NotBlank
    private String ipAddress;
    @NotBlank
    private String userAgent;
}
