package io.gsi.hive.platform.player.play;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.WebAppException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.event.EventType;
import io.gsi.hive.platform.player.exception.*;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.registry.ReconCounter;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.*;
import io.gsi.hive.platform.player.txn.event.*;
import io.micrometer.core.instrument.Tags;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Sql(statements = {PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PlayServiceIT extends ApiITBase {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @MockBean private MeshService meshService;
    @MockBean private SessionService sessionService;
    @MockBean private BonusWalletService bonusWalletService;
    @MockBean private ReconCounter reconCounter;


    @Autowired private TxnService txnService;
    @Autowired private PlayRepository playRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this.getClass());

        Session session = new Session();
        session.setAccessToken("testToken");
        when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
        when(meshService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(WalletBuilder.aWallet().build());
        when(bonusWalletService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(WalletBuilder.freeroundsWallet().build());
    }

    @After
    public void cleanUp() {
        if (playRepository.existsById(TxnPresets.PLAYID)) {
            playRepository.deleteById("1000-10");
        }
    }


    @Test
    public void givenStakeTxn_whenProcessTransaction_thenCreatePlay() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' AND" +
                        " num_txns = 1 AND stake = 20.00 AND win = 0 AND status = 'ACTIVE'"), is(1));
    }

    @Test
    public void givenWinTxn_whenProcessTransaction_thenPlayFinished() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        txnService.process(defaultWinTxnRequestBuilder().txnId("1000-2").build());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' AND" +
                        " num_txns = 2 AND stake = 20.00 AND win = 20.00  AND status = 'FINISHED'"), is(1));
    }

    @Test
    public void testOfTransactionExistingInDb() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        txnService.process(createStakeTxnWinRequest());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana'"), is(1));
    }

    @Test
    public void testOfCancelTransactionExistingInDb() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        txnService.cancel(createCancelStakeTxnWinRequest());

        verify(sessionService, times(1)).getSession(Mockito.anyString());
        verify(meshService).sendTxn(Mockito.anyString(), Mockito.any());
        verifyZeroInteractions(bonusWalletService);
        verify(meshService).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));
        verifyZeroInteractions(reconCounter);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'VOIDED' AND num_txns = 1"), is(1));
    }

    @Test
    public void givenApiError_whenCancelling_thenThrowExceptionAndMetricPublished() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        doThrow(new ApiKnownException("","")).when(meshService).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));

        assertThatThrownBy(() -> txnService.cancel(createCancelStakeTxnWinRequest()))
                .isInstanceOf(ApiException.class);

        verify(sessionService, times(1)).getSession(Mockito.anyString());
        verify(meshService).sendTxn(Mockito.anyString(), Mockito.any());
        verifyZeroInteractions(bonusWalletService);
        verify(meshService).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));
        verify(reconCounter).increment(Tags.of("Method","cancel","Error", "api_exception"));
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'ACTIVE' and num_txns = 1"), is(1));
    }

    @Test
    public void givenWebApp_whenCancelling_thenThrowExceptionAndMetricPublished() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        doThrow(new BadRequestException("")).when(meshService).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));

        assertThatThrownBy(() -> txnService.cancel(createCancelStakeTxnWinRequest()))
                .isInstanceOf(WebAppException.class);

        verify(sessionService, times(1)).getSession(Mockito.anyString());
        verify(meshService).sendTxn(Mockito.anyString(), Mockito.any());
        verifyZeroInteractions(bonusWalletService);
        verify(meshService).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));
        verify(reconCounter).increment(Tags.of("Method","cancel","Error", "web_app_exception"));
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'ACTIVE' and num_txns = 1"), is(1));
    }

    @Test
    public void givenExistingTransaction_whenMarkAsAutocompleted_thenSetAutocompletedToTrue() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        txnService.process(createStakeTxnWinRequest());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and auto_completed = 'false'"), is(1));

        String playId = "1000-10";
        playService.markPlayAsAutocompleted(playId);

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and auto_completed = 'false'"), is(0));

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and auto_completed = 'true'"), is(1));
    }

    @Test(expected = PlayNotFoundException.class)
    public void givenNonExistingTransaction_whenMarkAsAutocompleted_thenThrowPlayNotFoundException() {
        playService.markPlayAsAutocompleted("invalid-playId");
    }

    @Test
    public void testOfStake_Stake_CancelTransactionExistingInDb() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        txnService.cancel(createCancelStakeTxnWinRequest());

        verify(sessionService, times(1)).getSession(Mockito.anyString());
        verify(meshService).sendTxn(Mockito.anyString(), Mockito.any());
        verifyZeroInteractions(bonusWalletService);
        verify(meshService).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));
        verifyZeroInteractions(reconCounter);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'VOIDED'"), is(1));
    }

    @Test
    public void testOfStake_StakeTransactionExistingInDb() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'ACTIVE'"), is(1));
    }

    @Test
    public void testOfStake_StakeWin_StakeWinTransactionExistingInDb() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        txnService.process(createStakeTxnWinRequest());
        txnService.process(createStakeTxnWinRequest());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'FINISHED'"), is(1));
    }

    @Test
    public void testOfStake_StakeWin_StakeCancelTransactionExistingInDb() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        txnService.process(createStakeTxnWinRequest());
        txnService.cancel(createCancelStakeTxnWinRequest());

        verify(sessionService, times(2)).getSession(Mockito.anyString());
        verify(meshService, times(2)).sendTxn(Mockito.anyString(), Mockito.any());
        verify(meshService, times(2)).sendTxn(Mockito.anyString(), Mockito.any());
        verifyZeroInteractions(bonusWalletService);
        verify(meshService).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));
        verifyZeroInteractions(reconCounter);
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'VOIDED'"), is(1));
    }

    @Test
    public void testOfStake_ExternalCancelTransactionExistingInDb_shouldReturnFinished() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        txnService.process(createStakeTxnWinRequest());
        txnService.externalCancel(createCancelTxnWinRequest());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'VOIDED'"), is(1));
    }

    @Test
    public void testOfIdempotancyofUsingProcess() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        TxnReceipt receipt = txnService.process(defaultStakeTxnRequestBuilder().build());
        TxnReceipt receipt2 = txnService.process(defaultStakeTxnRequestBuilder().build());

        assertThat(receipt.getStatus(), is(receipt2.getStatus()));
        assertThat(receipt.getTxnId(), is(receipt2.getTxnId()));
        assertThat(receipt.getTxnRef(), is(receipt2.getTxnRef()));
        assertThat(receipt.getGameCode(), is(receipt2.getGameCode()));
    }

    @Test
    public void testOfIdempotancyofUsingCancel() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        Txn txn = txnService.cancel(createCancelStakeTxnWinRequest());
        Txn txn1 = txnService.cancel(createCancelStakeTxnWinRequest());


        assertThat(txn.getTxnId(), is(txn1.getTxnId()));
        assertThat(txn.getSessionId(), is(txn1.getSessionId()));
        assertThat(txn.getTxnRef(), is(txn1.getTxnRef()));
        assertThat(txn.getGameCode(), is(txn1.getGameCode()));
        assertThat(txn.getCcyCode(), is(txn1.getCcyCode()));
        assertThat(txn.getStatus(), is(txn1.getStatus()));
        assertThat(txn.getAmount(), is(txn1.getAmount()));

        verify(sessionService, times(1)).getSession(Mockito.anyString());
        verify(meshService).sendTxn(Mockito.anyString(), Mockito.any());
        verifyZeroInteractions(bonusWalletService);
        verify(meshService, times(2)).cancelTxn(Mockito.anyString(), Mockito.any(Txn.class), Mockito.any(TxnCancel.class));
        verifyZeroInteractions(reconCounter);
    }

    @Test
    public void testExternalCancelNegativeStakeBug() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        TxnCancelRequest txnCancelRequest = new TxnCancelRequestBuilder().build();
        TxnReceipt txnReceipt = txnService.externalCancel(txnCancelRequest);
        Play play = playRepository.findById(TxnPresets.PLAYID).orElseThrow();
        assertThat(play.getStake(), is(new BigDecimal("0.00")));
    }

    @Test
    public void testOfIdempotancyofExternalCancel() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());
        TxnReceipt firstCancelReceipt = txnService.externalCancel(createCancelTxnWinRequest());
        TxnReceipt secondCancelReceipt = txnService.externalCancel(createCancelTxnWinRequest());
        assertEquals(firstCancelReceipt.getTxnId(), secondCancelReceipt.getTxnId());
        assertEquals(firstCancelReceipt.getStatus(), secondCancelReceipt.getStatus());
        assertEquals(TxnStatus.CANCELLED, secondCancelReceipt.getStatus());
    }

    @Test
    public void testFailedStakeTxnGoesToVoidedPlay() {
        when(meshService.sendTxn(any(), any()))
                .then(
                        a -> {
                            throw new InsufficientFundsException("");
                        });
        try {
            txnService.process(defaultStakeTxnRequestBuilder().build());
        } catch (InsufficientFundsException ex) {
        }
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'VOIDED'"), is(1));
    }

    @Test
    public void testPendingStakeTxnGoesToActivePlay() {
        when(meshService.sendTxn(any(), any())).then(
                a -> {
                    throw new ApiTimeoutException("");
                });
        try {
            txnService.process(defaultStakeTxnRequestBuilder().build());
        } catch (ApiTimeoutException ex) {
        }
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and status = 'ACTIVE'"), is(1));
    }

    @Test
    public void savePlayRefs() {
        final var stakeTxn = TxnBuilder.txn()
                .withTxnId("1000-1")
                .withGameCode("testGame")
                .withPlayComplete(true)
                .withRoundComplete(false)
                .withPlayerId("player1")
                .withIgpCode("iguana")
                .withMode(Mode.real)
                .withGuest(false)
                .withCcyCode("GBP")
                .withSessionId("testSession")
                .withType(TxnType.STAKE)
                .withAmount(new BigDecimal("1.00"))
                .withPlayId("1000-10")
                .withPlayRef("not-1000-10")
                .build();

        playService.addTxn(stakeTxn);

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' and play_ref = 'not-1000-10'"), is(1));
    }

    @Test
    public void storePlayRefFromReply() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder
                .txnReceipt().withPlayRef("not-1000-10").build());

        txnService.process(defaultStakeTxnRequestBuilder().build());
        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_ref = 'not-1000-10'"), is(1));
    }

    @Test
    public void testGetPlay() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        txnService.process(defaultStakeTxnRequestBuilder().build());

        assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                " play_id = '1000-10' and player_id = 'player1' and igp_code = 'iguana' AND num_txns = 1" +
                        " AND stake = 20.00 AND win = 0 AND status = 'ACTIVE'"), is(1));

        Play play = playService.getPlay(TxnPresets.PLAYID);
        assertThat(play.getPlayId(), is(TxnPresets.PLAYID));
        assertThat(play.getStatus(), is(PlayStatus.ACTIVE));
    }

    @Test(expected = PlayNotFoundException.class)
    public void testGetPlayNotFound() {
        playService.getPlay("NOTFOUND");
    }

    @Test
    public void givenTxnWithHiveBonusFundEventDetails_whenAddTxn_thenPlayBonusFundTypeSetToHive() {
        BonusFundDetails hiveBonusFundDetails = new HiveBonusFundDetails();
        TxnEvent txnEvent = TxnRequest.builder().bonusFundDetails(hiveBonusFundDetails).build();
        Txn txn = TxnBuilder.txn().withTxnEvents(List.of(txnEvent)).build();

        playService.addTxn(txn);

        assertThat("play",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                "player_id = 'player1' and igp_code = 'iguana'"
                        + " AND num_txns = 1 AND bonus_fund_type = 'HIVE'"), is(1));
    }

    @Test
    public void givenTxnWithOperatorBonusFundEventDetails_whenAddTxn_thenPlayBonusTypeSetToOperator() {
        BonusFundDetails operatorBonusFundDetails = new OperatorBonusFundDetails();
        TxnEvent txnEvent = TxnRequest.builder().bonusFundDetails(operatorBonusFundDetails).build();
        Txn txn = TxnBuilder.txn().withTxnEvents(List.of(txnEvent)).build();

        playService.addTxn(txn);

        assertThat("play saved with operator bonus fund type",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                        "player_id = 'player1' and igp_code = 'iguana'"
                        + " AND num_txns = 1 AND bonus_fund_type = 'OPERATOR'"),
                is(1));
    }

    @Test
    public void givenTxnWithoutBonusFundEventDetails_whenAddTxn_thenPlayBonusTypeNotSet() {
        Txn txn = TxnBuilder.txn().build();

        playService.addTxn(txn);

        assertThat("play saved without a bonus fund type",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                        "player_id = 'player1' and igp_code = 'iguana'"
                                + " AND num_txns = 1 AND bonus_fund_type IS NULL"),
                is(1));
    }

    @Test
    public void givenTxnWithNullBonusFundEventDetails_whenAddTxn_thenPlayBonusTypeNotSet() {
        TxnEvent txnEvent = TxnRequest.builder().bonusFundDetails(null).build();
        Txn txn = TxnBuilder.txn().withTxnEvents(List.of(txnEvent)).build();

        playService.addTxn(txn);

        assertThat("play saved without a bonus fund type",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                        "player_id = 'player1' and igp_code = 'iguana'"
                                + " AND num_txns = 1 AND bonus_fund_type IS NULL"),
                is(1));
    }

    @Test
    public void givenTxnWithMultipleTxnEvents_whenAddTxn_thenPlayBonusSaved() {
        BonusFundDetails hiveBonusFundDetails = new HiveBonusFundDetails();
        TxnEvent txnRequestEvent = TxnRequest.builder().bonusFundDetails(hiveBonusFundDetails).build();
        TxnEvent txnReceiptEvent = new TxnReceipt();
        Txn txn = TxnBuilder.txn().withTxnEvents(List.of(txnRequestEvent, txnReceiptEvent)).build();

        playService.addTxn(txn);

        assertThat("play saved with a bonus fund type",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                        "player_id = 'player1' and igp_code = 'iguana'"
                                + " AND num_txns = 1 AND bonus_fund_type = 'HIVE'"),
                is(1));
    }

    private TxnRequest createStakeTxnWinRequest() {
        return defaultWinTxnRequestBuilder()
                .txnId(TxnPresets.TXNID_2)
                .build();
    }

    @Test
    public void givenStakeTxnWithSessionId_whenAddTxn_thenSessionIdSet() {
        Txn txn = TxnBuilder.txn().withType(TxnType.STAKE).withSessionId(SessionPresets.SESSIONID).build();

        playService.addTxn(txn);

        assertThat("play",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                        "player_id = 'player1' and igp_code = 'iguana'"
                                + " AND num_txns = 1 AND session_id = 'testSession'"), is(1));
    }

    @Test
    public void givenWinTxnWithSessionId_whenAddTxn_thenSessionIdNotSet() {
        addStakeWithoutSessionId();
        Txn txn = TxnBuilder.txn().withType(TxnType.WIN).withSessionId(SessionPresets.SESSIONID).build();

        playService.addTxn(txn);

        assertThat("play",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                        "player_id = 'player1' and igp_code = 'iguana'"
                                + " AND num_txns = 1 AND session_id is NULL"), is(1));
    }

    @Test
    public void givenStakeTxnWithoutSessionId_whenAddTxn_thenSessionIdNotSet() {
        addStakeWithoutSessionId();

        assertThat("play",
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "t_play",
                        "player_id = 'player1' and igp_code = 'iguana'"
                                + " AND num_txns = 1 AND session_id is NULL"), is(1));
    }

    @Test
    public void givenAWinTxnTwice_whenProcessTxn_thenIdempotentPlayUpdating() {
        when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
        TxnRequest winTxnRequest = defaultWinTxnRequestBuilder().txnId("1000-2").build();
        TxnRequest stakeTxnRequest = defaultStakeTxnRequestBuilder().build();

        txnService.process(stakeTxnRequest);
        txnService.process(winTxnRequest);
        txnService.process(winTxnRequest);

        Play play = playRepository.findById(TxnPresets.PLAYID)
            .orElseThrow(PlayNotFoundException::new);

        assertThat("Play has correct win amount", play.getWin(), is(winTxnRequest.getAmount()));
        assertThat("Play has correct number of transactions", play.getNumTxns(), is(2));
    }

    @Test
    public void givenAWinTxnTwice_whenUpdateFromTxnReceipt_thenIdempotentPlayUpdating() {
        Play savedPlay = PlayBuilder.play()
            .withStatus(PlayStatus.ACTIVE)
            .withNumTxns(1)
            .withWin(BigDecimal.ZERO)
            .build();
        Txn winTxn = TxnBuilder.txn().withType(TxnType.WIN).build();
        TxnReceipt winTxnReceipt = TxnReceiptBuilder.txnReceipt().build();
        playRepository.saveAndFlush(savedPlay);

        playService.updateFromTxnReceipt(winTxn, winTxnReceipt);
        playService.updateFromTxnReceipt(winTxn, winTxnReceipt);


        Play play = playRepository.findById(TxnPresets.PLAYID)
            .orElseThrow(PlayNotFoundException::new);

        assertThat("Play has correct win amount", play.getWin(), is(winTxn.getAmount()));
        assertThat("Play has correct number of transactions", play.getNumTxns(), is(2));
    }

    private TxnCancelRequest createCancelTxnWinRequest() {
        TxnCancelRequest txnCancelRequest = new TxnCancelRequest();
        txnCancelRequest.setTxnId("1000-1");
        txnCancelRequest.setGameCode("testGame");
        txnCancelRequest.setPlayComplete(true);
        txnCancelRequest.setRoundComplete(false);
        txnCancelRequest.setType(EventType.txnCancelRequest);
        return txnCancelRequest;
    }

    private Txn createCancelStakeTxnWinRequest() {
        return TxnBuilder.txn()
                .withTxnId("1000-1")
                .withGameCode("testGame")
                .withPlayComplete(true)
                .withRoundComplete(false)
                .withPlayerId("player1")
                .withIgpCode("iguana")
                .withMode(Mode.real)
                .withGuest(false)
                .withCcyCode("GBP")
                .withSessionId("testSession")
                .withType(TxnType.WIN)
                .withAmount(new BigDecimal("1.00"))
                .build();
    }

    private void addStakeWithoutSessionId() {
        Txn txn = TxnBuilder.txn().withSessionId(null).withType(TxnType.STAKE).build();
        playService.addTxn(txn);
    }
}
