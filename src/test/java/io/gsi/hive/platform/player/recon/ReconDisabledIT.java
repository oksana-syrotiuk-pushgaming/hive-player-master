package io.gsi.hive.platform.player.recon;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.autocompletion.*;
import io.gsi.hive.platform.player.recon.game.ReconCallbackSchedulingService;
import io.gsi.hive.platform.player.recon.game.ReconCallbackService;
import io.gsi.hive.platform.player.recon.game.ReconGameEndpoint;
import io.gsi.hive.platform.player.recon.game.ReconGamePollingService;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties={"hive.recon.enabled=false"})
@DirtiesContext
public class ReconDisabledIT extends ApiITBase {
    @Autowired
    private ApplicationContext context;

    @Test
    public void givenAutocompleteDisabledByProperty_whenApplicationStarts_autocompletionBeansNotPresent() {
        assertThatThrownBy(() -> context.getBean(ReconConfig.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(ReconService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(ReconGamePollingService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(ReconGameEndpoint.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(ReconCallbackService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(ReconCallbackSchedulingService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
