package io.gsi.hive.platform.player.recon.metrics;

import io.gsi.commons.monitoring.MeterPublisher;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.micrometer.core.instrument.Tags;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@ConditionalOnProperty(name = "hive.recon.monitoring.enabled", matchIfMissing = true, havingValue = "true")
public class ReconMetricsService {

    private static final Log logger = LogFactory.getLog(ReconMetricsService.class);

    private final int reconAge;

    private final TxnRepository txnRepository;
    private final MeterPublisher meterPublisher;

    private static final String METRICS_RECON = "total_txn_recon";
    private static final String METRICS_PENDING = "total_txn_pending";
    private static final String METRICS_RECON_PENDING = "total_txn_recon_pending";



    public ReconMetricsService(@Value("${hive.recon.monitoring.txnAlertAgeMins:60}") int reconAge, final TxnRepository txnRepository, final MeterPublisher meterPublisher) {
        this.reconAge = reconAge;
        this.txnRepository = txnRepository;
        this.meterPublisher = meterPublisher;
    }


    /**
     * This method monitors the status of Transactions in a given time frame and publishes related metrics.
     * In particular, it is interested in the number of transactions that are in a manual reconciliation,
     * pending state and the combination of both.
     */
    @Scheduled(initialDelayString = "${hive.recon.monitoring.scheduleMillis:3600000}", fixedDelayString = "${hive.recon.monitoring.scheduleMillis:3600000}")
    public void monitorTxnStatus() {
        logger.info(String.format("Recon metrics job started at: %s ", ZonedDateTime.now(ZoneId.of("UTC"))));
        final var age = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(this.reconAge);

        final var reconCount = this.txnRepository.countByStatusAndTxnTsBefore(TxnStatus.RECON, age);
        meterPublisher.publishGauge(METRICS_RECON, Tags.empty(), reconCount);

        final var pendingCount = this.txnRepository.countByStatusAndTxnTsBefore(TxnStatus.PENDING, age);
        meterPublisher.publishGauge(METRICS_PENDING, Tags.empty(), pendingCount);

        meterPublisher.publishGauge(METRICS_RECON_PENDING, Tags.empty(), reconCount + pendingCount);

        logger.info(
                String.format(
                        "Recon metrics job finished at: %s, total_txn_recon: %s, total_txn_pending: %s",
                        ZonedDateTime.now(ZoneId.of("UTC")),
                        reconCount,
                        pendingCount
                )
        );

    }

}
