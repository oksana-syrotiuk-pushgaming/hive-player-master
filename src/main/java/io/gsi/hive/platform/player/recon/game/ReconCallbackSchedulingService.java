package io.gsi.hive.platform.player.recon.game;

import io.gsi.commons.monitoring.ExceptionMonitorService;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.txn.TxnCallback;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.RequiredArgsConstructor;
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
 * This service should pull x locked records from the callback repo and send them
 */
@Service
@ConditionalOnProperty(name = "hive.recon.enabled", havingValue = "true", matchIfMissing = true)
public class ReconCallbackSchedulingService {
    private static final Log logger = LogFactory.getLog(ReconCallbackSchedulingService.class);

    //Setters at bottom of class
    private final ReconCallbackService callbackService;
    private final TxnCallbackRepository callbackRepository;
    private final Integer callbackBatchSize;
    private final Integer callbackRetries;
    private final MeterRegistry meterRegistry;
    private final ThreadPoolTaskExecutor callbackTaskExecutor;
    private final ExceptionMonitorService exceptionMonitorService;
    private final Integer callbackPause;
    private Timer batchTimer;

    public ReconCallbackSchedulingService(TxnCallbackRepository repository, ReconCallbackService reconCallbackService, ExceptionMonitorService exceptionMonitorService, MeterRegistry meterRegistry, @Value("${hive.recon.callbackRetries:5}") Integer retries, @Qualifier("callbackTaskExecutor") ThreadPoolTaskExecutor excecutor,
                                          @Value("${hive.recon.callbackBatchSize:30}") Integer size, @Value("${hive.recon.callbackBatchPause:10}") Integer pause) {
        this.callbackRepository = repository;
        this.callbackService = reconCallbackService;
        this.exceptionMonitorService = exceptionMonitorService;
        this.meterRegistry = meterRegistry;
        this.callbackRetries = retries;
        this.callbackTaskExecutor = excecutor;
        this.callbackBatchSize = size;
        this.callbackPause = pause;
    }

    @PostConstruct
    public void init() {
        batchTimer = meterRegistry.timer(this.getClass().getSimpleName(), "name", "batchTimer");
    }

    @Scheduled(initialDelayString = "${hive.recon.callback.initialDelay:30000}", fixedDelayString = "${hive.recon.callback.fixedDelay:30000}")
    public void processCallbacks() {
        Thread.currentThread().setName("rcn-cb-supervisor");
        logger.info("recon looking for queued callbacks...");
        while (true) {
            List<TxnCallback> txnCallbacks = callbackRepository.getQueuedCallbacks(callbackBatchSize, callbackRetries);
            if (txnCallbacks.isEmpty()) {
                break;
            }
            int c = txnCallbacks.size();
            final Instant batchTimerStart = Instant.now();
            try {
                Instant startBatch = Instant.now();
                logger.info(String.format("recon callbacks for batch of %d transactions", c));

                final CountDownLatch latch = new CountDownLatch(c);
                for (final TxnCallback txnCallback : txnCallbacks) {
                    callbackTaskExecutor.submit(() -> {
                        try {
                            callbackService.sendCallback(txnCallback.getTxnId());
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
                    logger.warn(String.format("recon callbacks interrupted"));
                }

                int size = callbackTaskExecutor.getThreadPoolExecutor().getQueue().size();
                if (size != 0) {
                    logger.warn(String.format("Recon threadpool queue is not empty, size=%d", size));
                }

                logger.info(String.format("finished batch of %d transactions, time taken=%d seconds", txnCallbacks.size(),
                        Duration.between(startBatch, Instant.now()).getSeconds()));
            } finally {
                batchTimer.record(Instant.now().toEpochMilli() - batchTimerStart.toEpochMilli(), TimeUnit.MILLISECONDS);
            }
            if (callbackPause > 0) {
                try {
                    Thread.sleep(callbackPause * 1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
