package io.gsi.hive.platform.player.registry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class ReconCounter {
    private static final String TXN_RECON_STATUS = "txn_recon_status";

    private static final Counter.Builder COUNTER_BUILDER = Counter.builder(TXN_RECON_STATUS);

    public void increment(Tags tags) {
        final var counter = ReconCounter.COUNTER_BUILDER.tags(tags).register(Metrics.globalRegistry);
        counter.increment();
    }
}
