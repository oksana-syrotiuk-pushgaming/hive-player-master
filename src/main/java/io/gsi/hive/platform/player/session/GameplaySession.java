package io.gsi.hive.platform.player.session;

import io.gsi.commons.validation.ValidCountry;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@DiscriminatorValue("2")
public class GameplaySession extends Session {

    @Column
    private String sessionToken;

    @Column
    private String authToken;

    @Column
    private String guestToken;

    @Column
    private String launchReferrer;

    @Column @Enumerated(EnumType.STRING)
    private ClientType clientType;

    @Column @ValidCountry
    private String country;

    @Column
    private String region;

    @Column
    private Boolean tokenUsed;

    public GameplaySession() {
        this.sessionToken = UUID.randomUUID().toString();
        this.tokenUsed = false;
    }
}
