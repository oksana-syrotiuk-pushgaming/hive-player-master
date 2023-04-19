package io.gsi.hive.platform.player.autocompletion;

import io.gsi.hive.platform.player.ApiITBase;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties={"hive.autocomplete.enabled=false"})
@DirtiesContext
public class AutocompleteDisabledIT extends ApiITBase {
    @Autowired
    private ApplicationContext context;

    @Test
    public void givenAutocompleteDisabledByProperty_whenApplicationStarts_autocompletionBeansNotPresent() {
        assertThatThrownBy(() -> context.getBean(AutocompleteEndpointConfig.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(AutocompleteGameEndpoint.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(AutocompleteQueueingScheduledService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(AutocompleteRequestScheduledService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean(AutocompleteRequestService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
