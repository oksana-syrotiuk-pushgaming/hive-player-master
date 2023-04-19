/**
 * © gsi.io 2015
 */
package io.gsi.hive.platform.player.recon;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import io.gsi.commons.monitoring.ExceptionMonitorService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * ReconService
 * 
 * This service picks up any pending stakes and reconciles them
 *
 */
@Service
@ConditionalOnProperty(name = "hive.recon.enabled", havingValue = "true", matchIfMissing = true)
public class ReconService {

	private static final Log logger = LogFactory.getLog(ReconService.class);

	private Integer batchSize;
	private Integer batchPause;
	private Integer maxRetries;
	private Integer offsetSeconds;
	
	private ExceptionMonitorService exceptionMonitorService;
	private MeterRegistry meterRegistry;
	private Timer batchTimer;
	
	private ReconTxnService reconTxnService;

	@Autowired @Qualifier("reconTaskExecutor")
	private ThreadPoolTaskExecutor reconTaskExecutor;

	public ReconService(ReconTxnService txnService, ExceptionMonitorService exceptionMonitorService, MeterRegistry meterRegistry,
						@Value("#{new java.lang.Integer('${hive.recon.txn.offsetSeconds:30}')}") Integer offsetSeconds, @Value("#{new java.lang.Integer('${hive.recon.txn.batchPause:30}')}") Integer batchPause,
						@Value("#{new java.lang.Integer('${hive.recon.txn.batchSize:500}')}") Integer batchSize, @Value("#{new java.lang.Integer('${hive.recon.maxRetries:10}')}") Integer maxRetries) {
		this.reconTxnService = txnService;
		this.exceptionMonitorService = exceptionMonitorService;
		this.meterRegistry = meterRegistry;
		this.offsetSeconds = offsetSeconds;
		this.batchPause = batchPause;
		this.batchSize = batchSize;
		this.maxRetries = maxRetries;
	}
	
	public Integer getMaxRetries() {
		return maxRetries;
	}

	@PostConstruct
	public void init() {
		batchTimer = meterRegistry.timer(this.getClass().getSimpleName(), "name", "batchTimer");
	}

	/**
	 * Looks for any pending stakes and wins and reconciles them
	 */
	@Scheduled(initialDelayString = "${hive.recon.scheduler.initialDelay:60000}", fixedDelayString = "${hive.recon.scheduler.fixedDelay:30000}")
	public void recon() {
		Thread.currentThread().setName("rcn-supervisor");
		logger.info("recon looking for pending transactions...");
		while (true) {
			ZonedDateTime beforeTimestamp = ZonedDateTime.ofInstant(Instant.now().minus(offsetSeconds, ChronoUnit.SECONDS), ZoneId.of("UTC"));

			List<String> txnIds = reconTxnService.getTxnsForRecon(beforeTimestamp, batchSize);
			if (txnIds.isEmpty()) {
				break;
			}
			Instant batchTimerStart = Instant.now();
			int c = txnIds.size();
			try {
				Instant startBatch = Instant.now();
				logger.info(String.format("recon for batch of %d transactions", c));

				final CountDownLatch latch = new CountDownLatch(c);
				for (final String txnId : txnIds) {
					reconTaskExecutor.submit(() -> {
						try {
							reconcileTxn(txnId);
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
					logger.warn(String.format("recon interrupted"));
				}

				int size = reconTaskExecutor.getThreadPoolExecutor().getQueue().size();
				if (size != 0) {
					logger.warn(String.format("Recon threadpool queue is not empty, size=%d", size));
				}

				logger.info(String.format("finished batch of %d transactions, time taken=%d seconds", txnIds.size(),
						Duration.between(startBatch, Instant.now()).getSeconds()));
			} finally {
				batchTimer.record(Instant.now().toEpochMilli() - batchTimerStart.toEpochMilli(), TimeUnit.MILLISECONDS);
			}
			if (batchPause > 0) {
				try {
					Thread.sleep(batchPause * 1000);
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	public void reconcileTxn(String txnId) {
		try {
			reconTxnService.reconcileTxnAndPlay(txnId);
		} catch (Exception e) {
			logger.error(String.format("failed to recon txnId=%s: %s", txnId, e.getMessage()));
			exceptionMonitorService.monitorException(e);
			reconTxnService.incrementRetry(txnId, maxRetries, e.getClass().getSimpleName());
		}
	}
	
}
