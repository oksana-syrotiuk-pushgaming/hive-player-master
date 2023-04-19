package io.gsi.hive.platform.player.autocompletion;

import io.gsi.commons.monitoring.ExceptionMonitorService;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This service should pull x records from the autocomplete request repo and send them
 */
@Service
@ConditionalOnProperty(name = "hive.autocomplete.enabled", havingValue = "true", matchIfMissing = true)
public class AutocompleteRequestScheduledService {
    private static final Log logger = LogFactory.getLog(AutocompleteRequestScheduledService.class);

    private final AutocompleteRequestService autocompleteRequestService;
    private final AutocompleteRequestRepository autocompleteRequestRepository;
    private final ExceptionMonitorService exceptionMonitorService;
    private final MeterRegistry meterRegistry;
    private final Integer autocompleteRetries;
    private final Integer autocompleteBatchSize;
    private final Integer requestPause;

    private Timer batchTimer;
    private final ThreadPoolTaskExecutor requestTaskExecutor;

    public AutocompleteRequestScheduledService(AutocompleteRequestService autocompleteRequestService,
                                               AutocompleteRequestRepository autocompleteRequestRepository,
                                               ExceptionMonitorService exceptionMonitorService,
                                               MeterRegistry meterRegistry,
                                               @Qualifier("autocompleteTaskExecutor") ThreadPoolTaskExecutor executor,
                                               @Value("${hive.autocomplete.autocompleteRetries:5}") Integer retries,
                                               @Value("${hive.autocomplete.autocompleteBatchSize:30}") Integer size,
                                               @Value("${hive.autocomplete.request.batchPause:10}") Integer pause) {
        this.autocompleteRequestService = autocompleteRequestService;
        this.autocompleteRequestRepository = autocompleteRequestRepository;
        this.exceptionMonitorService = exceptionMonitorService;
        this.meterRegistry = meterRegistry;
        this.autocompleteRetries = retries;
        this.autocompleteBatchSize = size;
        this.requestPause = pause;
        this.requestTaskExecutor = executor;
    }

    @PostConstruct
    public void init() {
        batchTimer = meterRegistry.timer(this.getClass().getSimpleName(), "name", "batchTimer");
    }

    @Scheduled(initialDelayString = "${hive.autocomplete.scheduler.request.initialDelay:60000}", fixedDelayString = "${hive.autocomplete.scheduler.request.fixedDelay:120000}")
    public void processRequests() {
        Thread.currentThread().setName("atc-rq-supervisor");
        logger.info("autocompletion looking for queued requests...");
        while (true) {
            List<AutocompleteRequest> autocompleteRequests = autocompleteRequestRepository.getQueuedRequests(autocompleteBatchSize, autocompleteRetries);
            if (autocompleteRequests.isEmpty()) {
                break;
            }
            int c = autocompleteRequests.size();
            final Instant batchTimerStart = Instant.now();
            try {
                Instant startBatch = Instant.now();
                logger.info(String.format("autocomplete requests for batch of %d transactions", c));

                final CountDownLatch latch = new CountDownLatch(c);
                for (final AutocompleteRequest autocompleteRequest : autocompleteRequests) {
                    requestTaskExecutor.submit(() -> {
                        try {
                            autocompleteRequestService.sendRequest(autocompleteRequest.getPlayId());
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                            exceptionMonitorService.monitorException(e);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                try {
                    latch.await();
                } catch (InterruptedException ie) {
                    logger.warn("autocomplete requests interrupted");
                }

                int size = requestTaskExecutor.getThreadPoolExecutor().getQueue().size();
                if (size != 0) {
                    logger.warn(String.format("Autocompletion threadpool queue is not empty, size=%d", size));
                }

                logger.info(String.format("finished batch of %d transactions, time taken=%d seconds", autocompleteRequests.size(),
                        Duration.between(startBatch, Instant.now()).getSeconds()));
            } finally {
                batchTimer.record(Instant.now().toEpochMilli() - batchTimerStart.toEpochMilli(), TimeUnit.MILLISECONDS);
            }
            if (requestPause > 0) {
                try {
                    Thread.sleep(requestPause * 1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}