package io.gsi.hive.platform.player.autocompletion;

import io.gsi.hive.platform.player.ApiITBase;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

public class AutocompleteEnabledIT extends ApiITBase {
    @Autowired
    private ApplicationContext context;

    @Test
    public void givenAutocompleteEnabledByProperty_whenApplicationStarts_autocompletionBeansPresent() {
        assertThat(context.getBean(AutocompleteEndpointConfig.class)).isNotNull();
        assertThat(context.getBean(AutocompleteGameEndpoint.class)).isNotNull();
        assertThat(context.getBean(AutocompleteQueueingScheduledService.class)).isNotNull();
        assertThat(context.getBean(AutocompleteRequestScheduledService.class)).isNotNull();
        assertThat(context.getBean(AutocompleteRequestService.class)).isNotNull();
    }
}
