package io.gsi.hive.platform.player.session;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hive.session.expiry")
public class SessionExpiryConfigProperties {
    private Integer batchSize = 300;

    private Boolean batched = true;
}