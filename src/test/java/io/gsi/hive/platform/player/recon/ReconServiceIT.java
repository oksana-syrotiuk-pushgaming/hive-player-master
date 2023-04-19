package io.gsi.hive.platform.player.recon;

import static io.gsi.hive.platform.player.txn.BonusFundDetailsPresets.defaultOperatorBonusFundDetails;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.autocompletion.AutocompleteRequest;
import io.gsi.hive.platform.player.builders.AutocompleteRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.exception.ApiException;
import io.gsi.hive.platform.player.exception.ApiTimeoutException;
import io.gsi.hive.platform.player.exception.FreeroundsFundNotAvailableException;
import io.gsi.hive.platform.player.exception.PlayerStatusException;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.AbstractTxn;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestClientException;


@Sql(statements={PersistenceITBase.CLEAN_DB_SQL,PersistenceITBase.PLAYER_SQL}, executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ReconServiceIT extends ApiITBase {

	@MockBean
	private TxnCallbackRepository txnCallbackRepository;
	@MockBean
	private MeshService meshService;
	@MockBean
	private SessionService sessionService;

	@Autowired private AutocompleteRequestRepository autocompleteRequestRepository;
	@Autowired private TxnService txnService;
	@Autowired private ReconTxnService txnReconService;
	@Autowired private ReconService reconService;
	@Autowired private TxnRepository txnRepository;

	@SpyBean private PlayRepository playRepository;


	@Before
	public void setup() {
		Session session = new Session();
		session.setAccessToken("testToken");
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
	}

	@After
	public void cleanUp() {
		Mockito.reset(playRepository);
	}

	@Test
	public void givenStakeTxn_whenReconcileTxn_thenStakeCanceled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFailAndSuccess();

		successfulStakeReconcile(defaultStakeTxnRequestBuilder().build());

		verify(meshService, times(1)).cancelTxn(any(), any(), any());
		assertThat("txn's play removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.empty())));
	}

	@Test
	public void givenStakeTxnAndApiTimeout_whenReconcileTxn_thenTxnPendingAndRetryCounterIncremented() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(defaultStakeTxnRequestBuilder().build());
		doThrow(new ApiTimeoutException("API_TIMEOUT"))
		.when(meshService).cancelTxn(any(), any(), any());

		reconService.reconcileTxn(txnId);

		verify(meshService, times(1)).cancelTxn(any(), any(), any());
		verify(txnCallbackRepository, times(0))
				.saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		List<String> reconTxnIds = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("1 transaction for recon", reconTxnIds.size(), is(1));

		AbstractTxn txn = txnRepository.findById(reconTxnIds.get(0)).get();
		assertThat(txn, is(notNullValue()));
		assertThat("transaction is still pending", txn.getStatus(), is(TxnStatus.PENDING));
		assertThat("retry counter was incremented", txn.getRetry(), is(1));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	@Test
	public void givenOperatorFreeRoundStake_whenReconcileTxn_thenOperatorFreeRoundTxnCanceled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendOperatorFreeroundsTxnForFailAndSuccess();

		OperatorBonusFundDetails operatorBonusFundDetails = defaultOperatorBonusFundDetails().build();
		TxnRequest operatorFreeroundsStakeRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(operatorBonusFundDetails)
				.build();

		successfulStakeReconcile(operatorFreeroundsStakeRequest);

		verify(meshService, times(1))
				.cancelOperatorFreeroundsTxn(any(), any(), any(), eq(operatorBonusFundDetails));
		assertThat("txn's play removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.empty())));
	}

	@Test
	public void givenWinTxn_whenReconcileTxn_thenTxnReconciled() {
		Play play = PlayBuilder.play()
				.withStatus(PlayStatus.ACTIVE)
				.withBonusFundType(OperatorBonusFundDetails.TYPE)
				.build();
		playRepository.saveAndFlush(play);
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFailAndSuccess();

		String txnId = addFailedTxn(defaultWinTxnRequestBuilder().build());

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("1 transactions for recon", reconTxns.size(), is(1));

		reconService.reconcileTxn(txnId);

		verify(meshService, times(2)).sendTxn(any(), any());
		verify(txnCallbackRepository).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions for recon", reconTxns.size(), is(0));
		assertThat("txn's play removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.empty())));
	}

	@Test
	public void giveWinTxnAndFreeroundsFundNotAvailableException_whenReconcileTxn_thenTxnFailed() {
		var play = PlayBuilder.play().withStatus(PlayStatus.ACTIVE).withGuest(false).build();
		when(playRepository.findById(Mockito.anyString())).thenReturn(Optional.ofNullable(play));

		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);

		when(meshService.sendTxn(any(), any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))
		.thenThrow(new FreeroundsFundNotAvailableException(""));

		String txnId = addFailedTxn(defaultWinTxnRequestBuilder().build());

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("1 transactions for recon", reconTxns.size(), is(1));

		reconService.reconcileTxn(txnId);

		verify(meshService, times(2)).sendTxn(any(), any());
		verify(txnCallbackRepository)
				.saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), Mockito.eq("FAILED"));

		reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions for recon", reconTxns.size(), is(0));
		assertThat("txn's play removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.empty())));
	}

	@Test
	public void givenWinTxnAndApiTimeout_whenReconcileTxn_thenTxnStillPendingAndIncrementRetry() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(defaultWinTxnRequestBuilder().build());

		reconService.reconcileTxn(txnId);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction still for recon", reconTxns.size(), is(1));
		AbstractTxn txn = txnRepository.findById(reconTxns.get(0)).get();
		assertThat("transaction is still pending", txn.getStatus(), is(TxnStatus.PENDING));
		assertThat("retry counter was incremented", txn.getRetry(), is(1));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	@Test
	public void givenWinTxnAndKnownApiException_whenReconcileTxn_thenTxnPutIntoManualRecon() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForTimeoutThenPlayerStatusException();
		String txnId = addFailedTxn(defaultWinTxnRequestBuilder().build());

		reconService.reconcileTxn(txnId);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction no longer for automatic recon", reconTxns.size(), is(0));
		AbstractTxn txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		assertThat("transaction is in manual recon", txn.getStatus(), is(TxnStatus.RECON));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	@Test
	public void givenOperatorFreeRoundWinTxn_whenReconcileTxn_thenTxnReconciled() {
		Play play = PlayBuilder.play()
				.withStatus(PlayStatus.ACTIVE)
				.withBonusFundType(OperatorBonusFundDetails.TYPE)
				.build();
		playRepository.saveAndFlush(play);
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendOperatorFreeroundsTxnForFailAndSuccess();

		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(defaultOperatorBonusFundDetails().build())
				.build();

		String txnId = addFailedTxn(winRequest);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("1 transactions for recon", reconTxns.size(), is(1));

		reconService.reconcileTxn(txnId);

		verify(meshService, times(2)).sendOperatorFreeroundsTxn(any(), any(), any());
		verify(txnCallbackRepository).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions for recon", reconTxns.size(), is(0));
		assertThat("txn's play removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.empty())));
	}

	@Test
	public void givenStakeTxnAndAuthException_whenCancelTxn_thenTxnStillInRecon() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		TxnRequest request = defaultStakeTxnRequestBuilder().build();
		setupApiClientSendTxnForFail();
		addFailedTxn(request);

		doThrow(new AuthorizationException("Auth Ex"))
				.when(meshService).cancelTxn(any(),any(),any());

		reconService.reconcileTxn(request.getTxnId());

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction no longer in recon queue", reconTxns.size(), is(0));
		AbstractTxn actualTxn = txnRepository.findById(request.getTxnId()).get();
		assertThat("transaction set for manual recon", actualTxn.getStatus(), is(TxnStatus.RECON));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	@Test
	public void givenStakeTxnAndRestClientException_whenCancelTxn_thenTxnStillInRecon() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		TxnRequest request = defaultStakeTxnRequestBuilder().build();
		setupApiClientSendTxnForFailAndSuccess();
		addFailedTxn(request);

		doThrow(new RestClientException("RestClient Ex"))
				.when(meshService).cancelTxn(any(), any(), any());

		String txnId = request.getTxnId();
		Txn txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		txnRepository.saveAndFlush(txn);

		reconService.reconcileTxn(txnId);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction still in recon", reconTxns.size(), is(1));
		AbstractTxn actualTxn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		assertThat("transaction is pending", actualTxn.getStatus(), is(TxnStatus.PENDING));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	//TODO: I'm not sure how valid this test is - I think the level of mocking doesn't emulate a real world scenario, and
	// Downstream recon now catches the problem I think this is testing (Game needs called back)
	@Test
	public void givenWinTxnProcessedAndException_whenSaveToCallbackQueue_thenTxnPendingAndRetryCountIncremented() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFailAndSuccess();
		String txnId = addFailedTxn(defaultWinTxnRequestBuilder().build());

		doThrow(new RuntimeException("exception in play recon"))
				.when(txnCallbackRepository)
				.saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		reconService.reconcileTxn(txnId);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction still for recon", reconTxns.size(), is(1));
		AbstractTxn txn = txnRepository.findById(reconTxns.get(0)).get();
		assertThat("transaction is still pending", txn.getStatus(), is(TxnStatus.PENDING));
		assertThat("retry counter was incremented", txn.getRetry(), is(1));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	@Test
	public void givenWinTxnReachesMaxRetries_whenReconcileTxn_thenTxnSetToManualRecon() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(defaultWinTxnRequestBuilder().build());

		for (int i=0; i<reconService.getMaxRetries(); i++) {
			reconService.reconcileTxn(txnId);
		}

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions to recon", reconTxns.size(), is(0));
		AbstractTxn txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		assertThat("transaction reconciliation was tried for max retries", txn.getRetry(), is(reconService.getMaxRetries()));
		assertThat("transaction is set for manual RECON", txn.getStatus(), is(TxnStatus.RECON));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	@Test
	public void givenStakeTxnReachesMaxRetries_whenReconcileTxn_thenTxnSetToManualRecon() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(defaultStakeTxnRequestBuilder().build());

		doThrow(new ApiTimeoutException("API_TIMEOUT"))
		.when(meshService).cancelTxn(any(), any(), any());

		for (int i=0; i<reconService.getMaxRetries(); i++) {
			reconService.reconcileTxn(txnId);
		}

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions to recon", reconTxns.size(), is(0));
		AbstractTxn txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		assertThat("transaction reconciliation was tried for max retries", txn.getRetry(), is(reconService.getMaxRetries()));
		assertThat("transaction is set for manual RECON", txn.getStatus(), is(TxnStatus.RECON));
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	@Test
	public void givenStakeTxnSuccessAndOverRetriesLimitAndMultipleCalls_whenReconcileTxn_thenTxnStillCanceled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		final int LotsOfRetries = 500;

		setupApiClientSendTxnForFailAndSuccess();
		String txnId = addFailedTxn(defaultStakeTxnRequestBuilder().build());

		var txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		txn.setRetry(LotsOfRetries);
		txnRepository.save(txn);

		reconService.reconcileTxn(txnId);
		txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		assertThat("transaction is closed", txn.getStatus(), is(TxnStatus.CANCELLED));

		reconService.reconcileTxn(txnId);
		txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		assertThat("transaction is still closed", txn.getStatus(), is(TxnStatus.CANCELLED));
		assertThat("txn's play removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.empty())));
	}

	@Test
	public void givenCancellingTxnStatus_whenReconcileTxn_thenTxnCanceled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		setupApiClientSendTxnForFailAndSuccess();
		String txnId = addFailedTxn(defaultStakeTxnRequestBuilder().build());

		var txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		txn.setStatus(TxnStatus.CANCELLING);
		txnRepository.save(txn);

		reconService.reconcileTxn(txnId);
		txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		assertThat("transaction is still canceled", txn.getStatus(), is(TxnStatus.CANCELLED));
		assertThat("txn's play removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.empty())));
	}

	@Test
	public void givenMissingTxnId_whenReconcileTxn_thenCancelTxnNotCalled() {
		AutocompleteRequest autocompleteRequest = AutocompleteRequestBuilder.autocompleteRequest().build();
		autocompleteRequestRepository.saveAndFlush(autocompleteRequest);
		String missingTxnId = "BadNoGoodNotValid";
		setupApiClientSendTxnForFailAndSuccess();

		reconService.reconcileTxn(missingTxnId);
		verify(meshService, times(0)).cancelTxn(any(), any(), any());
		assertThat("txn's play not removed from autocomplete request queue",
				autocompleteRequestRepository.findById(TxnPresets.PLAYID),
				is(equalTo(Optional.of(autocompleteRequest))));
	}

	private String addFailedTxn(TxnRequest request) {
		try {
			txnService.process(request);
		} catch (ApiException e) {
			assertThat(e.getCode(), is("ApiTimeout"));
		}

		String txnId = request.getTxnId();
		Txn txn = txnRepository.findById(txnId).orElseThrow(() -> new NotFoundException("txn not found"));
		txnRepository.save(txn);

		return txnId;
	}

	private void successfulStakeReconcile(TxnRequest txnRequest) {
		String txnId = addFailedTxn(txnRequest);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("1 transactions for recon", reconTxns.size(), is(1));

		reconService.reconcileTxn(txnId);

		verify(txnCallbackRepository, times(1))
				.saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		List<String> reconTxnKeys = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions to recon", reconTxnKeys.size(), is(0));
	}

	private void setupApiClientSendTxnForFailAndSuccess() {
		when(meshService.sendTxn(any(), any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))
		.thenAnswer(invocation -> {
			AbstractTxn txn = invocation.getArgument(1);
			return createOKTxnReceipt(txn.getTxnId(), txn.getGameCode());
		});
	}

	private void setupApiClientSendOperatorFreeroundsTxnForFailAndSuccess() {
		when(meshService.sendOperatorFreeroundsTxn(any(), any(), any()))
				.thenThrow(new ApiTimeoutException("API_TIMEOUT"))
				.thenAnswer(invocation -> {
					AbstractTxn txn = invocation.getArgument(1);
					return createOKTxnReceipt(txn.getTxnId(), txn.getGameCode());
				});
	}

	private void setupApiClientSendTxnForTimeoutThenPlayerStatusException() {
		when(meshService.sendTxn(any(), any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))
		.thenThrow(new PlayerStatusException("Player status ex"));
	}

	private void setupApiClientSendTxnForFail() {
		when(meshService.sendTxn(any(), any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"));
	}

	public TxnReceipt createOKTxnReceipt(String txnId, String gameCode) {
		return TxnReceiptBuilder.txnReceipt()
				.withStatus(TxnStatus.OK)
				.withTxnId(txnId)
				.withGameCode(gameCode)
				.withWallet(WalletBuilder.aWallet().build())
				.build();

	}
}

