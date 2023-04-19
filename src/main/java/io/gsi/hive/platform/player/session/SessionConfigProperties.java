package io.gsi.hive.platform.player.session;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hive.session")
public class SessionConfigProperties {
    private Integer expirySecs = 300;
}