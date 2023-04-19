package io.gsi.hive.platform.player.session;

import io.gsi.commons.validation.ValidLang;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SessionCreationLogin extends Login {
    @NotBlank
    protected String igpCode;
    protected String siteId;

    @ValidLang
    protected String lang;
    protected String jurisdiction;
    //Used to be rgsGameId
    @NotBlank
    protected String gameCode;
    @NotNull
    protected Mode mode;
    protected String currency;
    @NotBlank
    protected String ipAddress;
    @NotBlank
    protected String userAgent;
    @NotNull
    protected ClientType clientType;
}
