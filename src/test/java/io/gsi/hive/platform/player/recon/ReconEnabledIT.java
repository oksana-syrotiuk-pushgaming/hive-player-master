package io.gsi.hive.platform.player.recon;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.autocompletion.*;
import io.gsi.hive.platform.player.recon.game.ReconCallbackSchedulingService;
import io.gsi.hive.platform.player.recon.game.ReconCallbackService;
import io.gsi.hive.platform.player.recon.game.ReconGameEndpoint;
import io.gsi.hive.platform.player.recon.game.ReconGamePollingService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

public class ReconEnabledIT extends ApiITBase {
    @Autowired
    private ApplicationContext context;

    @Test
    public void givenAutocompleteEnabledByProperty_whenApplicationStarts_autocompletionBeansPresent() {
        assertThat(context.getBean(ReconConfig.class)).isNotNull();
        assertThat(context.getBean(ReconService.class)).isNotNull();
        assertThat(context.getBean(ReconGamePollingService.class)).isNotNull();
        assertThat(context.getBean(ReconGameEndpoint.class)).isNotNull();
        assertThat(context.getBean(ReconCallbackService.class)).isNotNull();
        assertThat(context.getBean(ReconCallbackSchedulingService.class)).isNotNull();
    }
}
