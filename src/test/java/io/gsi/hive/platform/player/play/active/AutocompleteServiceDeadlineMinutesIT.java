package io.gsi.hive.platform.player.play.active;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.autocompletion.AutocompleteQueueingScheduledService;
import io.gsi.hive.platform.player.autocompletion.AutocompleteRequest;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.TxnType;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@TestPropertySource(properties={
    "hive.autocomplete.completionDeadlineMinutes={iguana:60}",
    "hive.autocomplete.algorithm=legacy"})
public class AutocompleteServiceDeadlineMinutesIT extends PersistenceITBase {

    @Autowired
    private AutocompleteQueueingScheduledService autocompleteQueueingScheduledService;
    @Autowired
    private AutocompleteRequestRepository autocompleteRequestRepository;
    @MockBean
    private PlayRepository playRepository;
    @MockBean
    private TxnRepository txnRepository;

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void givenDeadLineMinutesConfiguration_whenPlaysWaitingForAutocompletion_thenDeadlineServiceIsUsed() {
        Mockito.when(playRepository.findAllByStatusAndIgpCodeAndCreatedAtBefore(any(), any(), any()))
                .thenReturn(List.of(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                    List.of(
                            TxnBuilder.txn()
                                    .withTxnId("transaction_1")
                                    .withSessionId(SessionPresets.SESSIONID)
                                    .withType(TxnType.STAKE)
                                    .build()
                    )
            );

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests).hasSize(1);
        assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
        assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);

        Mockito.verify(txnRepository, Mockito.times(1)).findByPlayIdAndTypeIn(TxnPresets.PLAYID,
                List.of(TxnType.STAKE, TxnType.OPFRSTK));
        Mockito.verify(playRepository, Mockito.times(1)).findAllByStatusAndIgpCodeAndCreatedAtBefore(any(),any(), any());
        Mockito.verify(playRepository, Mockito.times(0)).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(),any());

    }

}
