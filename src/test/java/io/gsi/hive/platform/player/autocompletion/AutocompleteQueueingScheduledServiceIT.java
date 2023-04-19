package io.gsi.hive.platform.player.autocompletion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.play.active.ActivePlayServiceDeadlineMinutes;
import io.gsi.hive.platform.player.play.active.ActivePlayServiceTimePeriod;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.txn.TxnType;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@TestPropertySource(properties={"hive.autocomplete.algorithm=legacy"})
public class AutocompleteQueueingScheduledServiceIT extends PersistenceITBase {
    @Autowired
    private AutocompleteRequestRepository autocompleteRequestRepository;
    @MockBean
    private TxnRepository txnRepository;
    @MockBean
    private PlayRepository playRepository;
    @Autowired
    private ActivePlayServiceDeadlineMinutes activePlayServiceDeadlineMinutes;
    @Autowired
    private ActivePlayServiceTimePeriod activePlayServiceTimePeriod;
    @Autowired
    private AutocompleteQueueingScheduledService autocompleteQueueingScheduledService;

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void  givenBothTimePeriodAndDeadlineForDifferentIGPs_whenPlaysWaiting_thenPlaysQueued() {
        Mockito.when(playRepository.findAllByStatusAndIgpCodeAndCreatedAtBefore(any(), any(), any())).thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any())).thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).withIgpCode(
            IgpPresets.IGPCODE_GECKO).withPlayId(TxnPresets.PLAYID_2).build()));

        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_1")
                        .withSessionId(SessionPresets.SESSIONID)
                        .withType(TxnType.STAKE)
                        .build()
                )
            )
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_2")
                        .withSessionId(SessionPresets.SESSIONID_2)
                        .withType(TxnType.STAKE)
                        .build()
                )
            );

        Map<String,String> timePeriodMap = new HashMap<>();
        String after = OffsetTime.now(ZoneId.of("UTC+01:00")).minusHours(1).toString();
        String before = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(2).toString();
        timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
        activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

        Map<String,Integer> deadlineMinutesMap = new HashMap<>();
        deadlineMinutesMap.put(IgpPresets.IGPCODE_IGUANA, 60);
        activePlayServiceDeadlineMinutes.setCompletionDeadlineMinutes(deadlineMinutesMap);

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

        Mockito.verify(txnRepository, Mockito.times(1)).findByPlayIdAndTypeIn(TxnPresets.PLAYID, List.of(TxnType.STAKE, TxnType.OPFRSTK));
        Mockito.verify(txnRepository, Mockito.times(1)).findByPlayIdAndTypeIn(TxnPresets.PLAYID_2, List.of(TxnType.STAKE, TxnType.OPFRSTK));

        Mockito.verify(playRepository, Mockito.times(1)).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(),any());
        Mockito.verify(playRepository, Mockito.times(1)).findAllByStatusAndIgpCodeAndCreatedAtBefore(any(),any(), any());

        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests.size()).isEqualTo(2);
        assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
        assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);
        assertThat(queuedRequests.get(1).getPlayId()).isEqualTo(TxnPresets.PLAYID_2);
        assertThat(queuedRequests.get(1).getSessionId()).isEqualTo(SessionPresets.SESSIONID_2);

    }

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void givenDeadlineMinutes_whenPlaysWaiting_thenPlaysQueued() {

        Mockito.when(playRepository.findAllByStatusAndIgpCodeAndCreatedAtBefore(any(), any(), any())).thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_1")
                        .withSessionId(SessionPresets.SESSIONID)
                        .withType(TxnType.STAKE)
                        .build()
                )
            );
        Map<String,Integer> deadlineMinutesMap = new HashMap<>();
        deadlineMinutesMap.put(IgpPresets.IGPCODE_IGUANA, 60);
        activePlayServiceDeadlineMinutes.setCompletionDeadlineMinutes(deadlineMinutesMap);

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();


        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests.size()).isEqualTo(1);
        assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
        assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);

        Mockito.verify(txnRepository, Mockito.times(1)).findByPlayIdAndTypeIn(TxnPresets.PLAYID, List.of(TxnType.STAKE, TxnType.OPFRSTK));
        Mockito.verify(playRepository, Mockito.times(1)).findAllByStatusAndIgpCodeAndCreatedAtBefore(any(),any(), any());

    }

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void givenTimePeriod_whenWithinTimePeriod_thenPlaysQueued() {

        Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any())).thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_1")
                        .withSessionId(SessionPresets.SESSIONID)
                        .withType(TxnType.STAKE)
                        .build()
                )
            );

        Map<String,String> timePeriodMap = new HashMap<>();
        String after = OffsetTime.now(ZoneId.of("UTC+01:00")).minusHours(1).toString();
        String before = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(2).toString();
        timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
        activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

        Map<String,Integer> deadlineMinutesMap = new HashMap<>();
        activePlayServiceDeadlineMinutes.setCompletionDeadlineMinutes(deadlineMinutesMap);

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests.size()).isEqualTo(1);
        assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
        assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);

        Mockito.verify(txnRepository, Mockito.times(1)).findByPlayIdAndTypeIn(TxnPresets.PLAYID, List.of(TxnType.STAKE, TxnType.OPFRSTK));
        Mockito.verify(playRepository, Mockito.times(1)).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(),any());
    }

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void givenTimePeriod_whenBeforeTimePeriodWithPlaysWaiting_thenNoPlaysQueued() {
        Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any()))
                .thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_1")
                        .withSessionId(SessionPresets.SESSIONID)
                        .withType(TxnType.STAKE)
                        .build()
                )
            );

        Map<String,String> timePeriodMap = new HashMap<>();
        String after = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(1).toString();
        String before = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(2).toString();
        timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
        activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

        Map<String,Integer> deadlineMinutesMap = new HashMap<>();
        activePlayServiceDeadlineMinutes.setCompletionDeadlineMinutes(deadlineMinutesMap);

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests.size()).isEqualTo(0);

        Mockito.verify(txnRepository, Mockito.times(0)).findByPlayIdAndTypeIn(TxnPresets.PLAYID, List.of(TxnType.STAKE, TxnType.OPFRSTK));
        Mockito.verify(playRepository, Mockito.times(0)).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(),any());
    }

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void givenTimePeriod_whenAfterTimePeriodWithPlaysWaiting_thenNoPlaysQueued() {

        Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any())).thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_1")
                        .withSessionId(SessionPresets.SESSIONID)
                        .withType(TxnType.STAKE)
                        .build()
                )
            );

        Map<String,String> timePeriodMap = new HashMap<>();
        String after = OffsetTime.now(ZoneId.of("UTC+01:00")).minusHours(2).toString();
        String before = OffsetTime.now(ZoneId.of("UTC+01:00")).minusHours(1).toString();
        timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
        activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

        Map<String,Integer> deadlineMinutesMap = new HashMap<>();
        activePlayServiceDeadlineMinutes.setCompletionDeadlineMinutes(deadlineMinutesMap);

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests.size()).isEqualTo(0);

        Mockito.verify(txnRepository, Mockito.times(0)).findByPlayIdAndTypeIn(TxnPresets.PLAYID, List.of(TxnType.STAKE, TxnType.OPFRSTK));
        Mockito.verify(playRepository, Mockito.times(0)).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(),any());
    }

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void givenTimePeriodUTC8_whenPlaysWaiting_thenTimePeriodIsUsed() {

        Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any())).thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_1")
                        .withSessionId(SessionPresets.SESSIONID)
                        .withType(TxnType.STAKE)
                        .build()
                )
            );

        Map<String,String> timePeriodMap = new HashMap<>();
        String after = OffsetTime.now(ZoneId.of("UTC+08:00")).minusHours(1).toString();
        String before = OffsetTime.now(ZoneId.of("UTC+08:00")).plusHours(1).toString();
        timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + "," + before);
        activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

        Map<String,Integer> deadlineMinutesMap = new HashMap<>();
        activePlayServiceDeadlineMinutes.setCompletionDeadlineMinutes(deadlineMinutesMap);

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests.size()).isEqualTo(1);
        assertThat(queuedRequests.get(0).getPlayId()).isEqualTo(TxnPresets.PLAYID);
        assertThat(queuedRequests.get(0).getSessionId()).isEqualTo(SessionPresets.SESSIONID);


        Mockito.verify(playRepository, Mockito.times(0)).findAllByStatusAndIgpCodeAndCreatedAtBefore(any(),any(), any());
        Mockito.verify(playRepository, Mockito.times(1)).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(),any());
    }

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test(expected = InvalidStateException.class)
    public void givenTimePeriod_whenInvalidConfiguration_thenThrowException() {

        Map<String, String> timePeriodMap = new HashMap<>();
        String after = OffsetTime.now(ZoneId.of("UTC+01:00")).minusHours(1).toString();
        String before = OffsetTime.now(ZoneId.of("UTC+01:00")).plusHours(2).toString();
        timePeriodMap.put(IgpPresets.IGPCODE_GECKO, after + " " + before);
        activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);
    }

    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements={CLEAN_DB_SQL}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void givenNoConfiguration_whenPlaysWaiting_thenNoQueuedPlays() {

        Mockito.when(playRepository.findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(), any())).thenReturn(Arrays.asList(PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build()));
        Mockito.when(txnRepository.findByPlayIdAndTypeIn(any(), any()))
            .thenReturn(
                Arrays.asList(
                    TxnBuilder.txn()
                        .withTxnId("transaction_1")
                        .withSessionId(SessionPresets.SESSIONID)
                        .withType(TxnType.STAKE)
                        .build()
                )
            );

        Map<String,String> timePeriodMap = new HashMap<>();
        activePlayServiceTimePeriod.setCompletionTimePeriod(timePeriodMap);

        Map<String,Integer> deadlineMinutesMap = new HashMap<>();
        activePlayServiceDeadlineMinutes.setCompletionDeadlineMinutes(deadlineMinutesMap);

        autocompleteQueueingScheduledService.getActivePlaysAndQueueAutocompletion();

        List<AutocompleteRequest> queuedRequests = autocompleteRequestRepository.findAll();
        assertThat(queuedRequests.size()).isEqualTo(0);

        Mockito.verify(playRepository, Mockito.times(0)).findAllByStatusAndIgpCodeAndCreatedAtBefore(any(),any(), any());
        Mockito.verify(playRepository, Mockito.times(0)).findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(any(),any());
    }
}
