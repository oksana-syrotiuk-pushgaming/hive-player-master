package io.gsi.hive.platform.player.registry;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class MicrometerRegistryConfig {
    @EventListener(ApplicationReadyEvent.class)

    public void addPrometheusMeterRegistry() {
        Metrics.addRegistry(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));
    }
}
