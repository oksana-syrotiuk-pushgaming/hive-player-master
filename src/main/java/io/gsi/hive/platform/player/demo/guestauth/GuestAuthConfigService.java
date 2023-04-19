package io.gsi.hive.platform.player.demo.guestauth;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Setter
@Service
@ConfigurationProperties(prefix = "hive.guestauth")
public class GuestAuthConfigService {

    private List<String> igpCodes = new ArrayList<>();
    
    public boolean isGuestValidationEnabledForIgp(String igpCode) {
        return igpCodes != null && igpCodes.contains(igpCode);
    }
}