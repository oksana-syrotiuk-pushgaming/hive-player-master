package io.gsi.hive.platform.player.mesh.igpservicelocator;

import io.gsi.hive.platform.player.ApiITBase;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "hive.scheduling.enabled=true",
        "hive.igpService.locator.cache.repopulation.intervalMillis=50",
        "hive.igpService.locator.cache.repopulation.initialDelayMillis=0",
})
public class IgpServiceLocationCacheSchedulingIT extends ApiITBase {

    @SpyBean
    private IgpServiceLocationCache igpServiceLocationCache;

    @Test
    public void igpServiceLocationCache_populatingAtLeastFiveIgpServiceCodes_atMostOneSecond() {
        doNothing().when(igpServiceLocationCache).populateIgpServiceCodes();
        await()
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(() -> verify(igpServiceLocationCache,
                    atLeast(5)).populateIgpServiceCodes());
    }
}
