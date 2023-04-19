package io.gsi.hive.platform.player.recon.metrics;

import io.gsi.commons.monitoring.MeterPublisher;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.micrometer.core.instrument.Tags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL},
        executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements={PersistenceITBase.CLEAN_DB_SQL}, executionPhase= Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class ReconMetricsServiceIT extends ApiITBase {

    @MockBean
    private MeterPublisher meterPublisher;

    @Autowired
    private ReconMetricsService reconMetricsService;

    private final ZonedDateTime age = ZonedDateTime.now(ZoneId.of("UTC"));

    /**
     * Cases that should NOT be counted in the metrics job:
     *      - txnTs date is too young compared to the job min age and status RECON
     *      - txnTs date is too young compared to the job min age and status PENDING
     *      - txnTs date is older than the job min age but status OK
     *      - txnTs date is older than the job min age but status FAILED
     *      - txnTs date is older than the job min age but status CANCELLED
     *      - txnTs date is older than the job min age but status CANCELLING
     *      - txnTs date is older than the job min age but status NOTFOUND
     *
     * Cases that should be counted in the metrics job:
     *      - txnTs date is older than the job min age and status RECON x2
     *      - txnTs date is older than the job min age and status PENDING x2
     *
     * A case is counted if txn.txnTs < age - txnAlertAgeMinutes
     * hive.recon.monitoring.txnAlertAgeMinutes can be found in the test properties file
     */
    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this.getClass());

        var txnDateTooYoungStatusRecon = TxnBuilder.txn()
                .withTxnId("1015-0101-0001")
                .withStatus(TxnStatus.RECON)
                .withTxnTs(this.age.minusMinutes(5))
                .build();
        var txnDateTooYoungStatusPending = TxnBuilder.txn()
                .withTxnId("1015-0101-0002")
                .withStatus(TxnStatus.PENDING)
                .withTxnTs(this.age.minusMinutes(5))
                .build();
        var txnDateOkStatusOk = TxnBuilder.txn()
                .withTxnId("1015-0101-0003")
                .withStatus(TxnStatus.OK)
                .withTxnTs(this.age.minusMinutes(15))
                .build();
        var txnDateOkStatusFailed = TxnBuilder.txn()
                .withTxnId("1015-0101-0004")
                .withStatus(TxnStatus.FAILED)
                .withTxnTs(this.age.minusMinutes(15))
                .build();
        var txnDateOkStatusCancelled = TxnBuilder.txn()
                .withTxnId("1015-0101-0005")
                .withStatus(TxnStatus.CANCELLED)
                .withTxnTs(this.age.minusMinutes(15))
                .build();
        var txnDateOkStatusCancelling = TxnBuilder.txn()
                .withTxnId("1015-0101-0006")
                .withStatus(TxnStatus.CANCELLING)
                .withTxnTs(this.age.minusMinutes(15))
                .build();
        var txnDateOkStatusNotFound = TxnBuilder.txn()
                .withTxnId("1015-0101-0007")
                .withStatus(TxnStatus.NOTFOUND)
                .withTxnTs(this.age.minusMinutes(15))
                .build();

        var txnDateOkStatusRecon1 = TxnBuilder.txn()
                .withTxnId("1015-0101-0008")
                .withStatus(TxnStatus.RECON)
                .withTxnTs(this.age.minusMinutes(15))
                .build();
        var txnDateOkStatusRecon2 = TxnBuilder.txn()
                .withTxnId("1015-0101-0009")
                .withStatus(TxnStatus.RECON)
                .withTxnTs(this.age.minusMinutes(15))
                .build();
        var txnDateOkStatusPending1 = TxnBuilder.txn()
                .withTxnId("1015-0101-0010")
                .withStatus(TxnStatus.PENDING)
                .withTxnTs(this.age.minusMinutes(15))
                .build();
        var txnDateOkStatusPending2 = TxnBuilder.txn()
                .withTxnId("1015-0101-0011")
                .withStatus(TxnStatus.PENDING)
                .withTxnTs(this.age.minusMinutes(15))
                .build();

        this.saveTxn(
                txnDateTooYoungStatusRecon,
                txnDateTooYoungStatusPending,
                txnDateOkStatusOk,
                txnDateOkStatusFailed,
                txnDateOkStatusCancelled,
                txnDateOkStatusCancelling,
                txnDateOkStatusNotFound,
                txnDateOkStatusRecon1,
                txnDateOkStatusRecon2,
                txnDateOkStatusPending1,
                txnDateOkStatusPending2
        );
    }

    @Test
    public void givenTxnSaved_whenExecutingReconMetrics_thenMetricsPublished() {
        doNothing().when(this.meterPublisher).publishGauge(anyString(), any(), any());
        this.reconMetricsService.monitorTxnStatus();

        verify(this.meterPublisher).publishGauge("total_txn_recon", Tags.empty(), 2L);
        verify(this.meterPublisher).publishGauge("total_txn_pending", Tags.empty(), 2L);
        verify(this.meterPublisher).publishGauge("total_txn_recon_pending", Tags.empty(), 4L);
        verifyNoMoreInteractions(this.meterPublisher);
    }

}
