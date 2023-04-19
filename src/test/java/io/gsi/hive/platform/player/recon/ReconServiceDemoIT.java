package io.gsi.hive.platform.player.recon;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gsi.commons.monitoring.ExceptionMonitorService;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.demo.gateway.DemoWalletGateway;
import io.gsi.hive.platform.player.exception.ApiException;
import io.gsi.hive.platform.player.exception.ApiTimeoutException;
import io.gsi.hive.platform.player.exception.PlayerStatusException;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.AbstractTxn;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

/**This class mirrors the recon scenarios in ReconServiceIt, but for Guest Demo play*/
@SuppressWarnings("OptionalGetWithoutIsPresent")
@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
		statements = {PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL})
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, statements = PersistenceITBase.CLEAN_DB_SQL)
public class ReconServiceDemoIT extends ApiITBase {

	@MockBean
	private TxnCallbackRepository txnCallbackRepository;
	@MockBean
	private SessionService sessionService;
	@MockBean
	private DemoWalletGateway demoWalletGateway;
	@MockBean
	private ExceptionMonitorService exceptionMonitorService;

	@Autowired
	private TxnService txnService;

	@Autowired
	private ReconTxnService txnReconService;
	@Autowired
	private ReconService reconService;
	@Autowired
	private TxnRepository txnRepository;
	@Autowired
	private PlayRepository playRepository;


	@Before
	public void setup() {
		playerRepository.saveAndFlush(PlayerBuilder.aPlayer().withGuest(true).build());

		Session session = new Session();
		session.setAccessToken("testToken");
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
	}

	@Test
	public void successfulStakeReconcile() {
		setupApiClientSendTxnForFailAndSuccess();
		String txnId = addFailedTxn(createStakeTxnRequest());

		reconService.reconcileTxn(txnId);

		verify(txnCallbackRepository, times(1)).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());
		verify(demoWalletGateway, times(1)).cancelTxn(any(), any());

		List<String> reconTxnKeys = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions to recon", reconTxnKeys.size(), is(0));
	}

	@Test
	public void stakeReconWithError_TxnPendingAndIncrementRetry(){
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(createStakeTxnRequest());
		doThrow(new ApiTimeoutException("API_TIMEOUT"))
		.when(demoWalletGateway).cancelTxn(any(), any());

		reconService.reconcileTxn(txnId);

		verify(demoWalletGateway, times(1)).cancelTxn(any(), any());
		verify(txnCallbackRepository, times(0)).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		List<String> reconTxnIds = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("1 transaction for recon", reconTxnIds.size(), is(1));

		AbstractTxn txn = txnRepository.findById(reconTxnIds.get(0)).get();
		assertThat(txn, is(notNullValue()));
		assertThat("transaction is still pending", txn.getStatus(), is(TxnStatus.PENDING));
		assertThat("retry counter was incremented", txn.getRetry(), is(1));
	}


	@Test
	public void successfulWinTxnReconcile() {
		Play play = PlayBuilder.play()
				.withBonusFundType(OperatorBonusFundDetails.TYPE)
				.withStatus(PlayStatus.ACTIVE)
				.withGuest(true)
				.build();
		playRepository.saveAndFlush(play);
		setupApiClientSendTxnForFailAndSuccess();
		String txnId = addFailedTxn(createWinTxnRequest());

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("1 transactions for recon", reconTxns.size(), is(1));

		reconService.reconcileTxn(txnId);

		verify(demoWalletGateway, times(2)).processTxn(any());
		verify(txnCallbackRepository).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions for recon", reconTxns.size(), is(0));
	}

	@Test
	public void whenWinTrxReconFailed_TxnStillPendingAndIncrementRetry() {
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(createWinTxnRequest());

		reconService.reconcileTxn(txnId);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction still for recon", reconTxns.size(), is(1));
		AbstractTxn txn = txnRepository.findById(reconTxns.get(0)).get();
		assertThat("transaction is still pending", txn.getStatus(), is(TxnStatus.PENDING));
		assertThat("retry counter was incremented", txn.getRetry(), is(1));
	}

	@Test
	public void whenWinTrxReconApiKnownEx_TxnRecon() {
		setupApiClientSendTxnForTimeoutThenPlayerStatusException();
		String txnId = addFailedTxn(createWinTxnRequest());

		reconService.reconcileTxn(txnId);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction no longer for recon", reconTxns.size(), is(0));
		AbstractTxn txn = txnRepository.findById(txnId).get();
		assertThat("transaction is failed", txn.getStatus(), is(TxnStatus.RECON));
	}


	//Not sure if this is a valid test - see comment in ReconServiceIT
	@Test
	public void whenWinTrxRetryOkButPlayReconFailed_TxnStillPendingAndIncrementRetry() {
		setupApiClientSendTxnForFailAndSuccess();
		String txnId = addFailedTxn(createWinTxnRequest());

		Mockito.doThrow(new RuntimeException("exception in play recon")).when(txnCallbackRepository).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		reconService.reconcileTxn(txnId);

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("transaction still for recon", reconTxns.size(), is(1));
		AbstractTxn txn = txnRepository.findById(reconTxns.get(0)).get();
		assertThat("transaction is still pending", txn.getStatus(), is(TxnStatus.PENDING));
		assertThat("retry counter was incremented", txn.getRetry(), is(1));
	}

	@Test
	public void whenWinTxnRetryFailsForMaxRetries_TxnIsSetToManualRECON() {
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(createWinTxnRequest());

		for (int i=0; i<reconService.getMaxRetries(); i++) {
			reconService.reconcileTxn(txnId);
		}

		Mockito.verify(exceptionMonitorService, times(0)).monitorException(Mockito.isA(IllegalStateException.class));

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions to recon", reconTxns.size(), is(0));
		AbstractTxn txn = txnRepository.findById(txnId).get();
		assertThat("transaction reconciliation was tried for max retries", txn.getRetry(), is(reconService.getMaxRetries()));
		assertThat("transaction is set for manual RECON", txn.getStatus(), is(TxnStatus.RECON));
	}

	@Test
	public void whenStakeTxnRetryFailsForMaxRetries_TxnIsSetToManualRECON(){
		setupApiClientSendTxnForFail();
		String txnId = addFailedTxn(createStakeTxnRequest());

		doThrow(new ApiTimeoutException("API_TIMEOUT"))
		.when(demoWalletGateway).cancelTxn(any(), any());

		for (int i=0; i<reconService.getMaxRetries(); i++) {
			reconService.reconcileTxn(txnId);
		}

		Mockito.verify(exceptionMonitorService, times(0)).monitorException(Mockito.isA(IllegalStateException.class));

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions to recon", reconTxns.size(), is(0));
		AbstractTxn txn = txnRepository.findById(txnId).get();
		assertThat("transaction reconciliation was tried for max retries", txn.getRetry(), is(reconService.getMaxRetries()));
		assertThat("transaction is set for manual RECON", txn.getStatus(), is(TxnStatus.RECON));
	}

	@Test
	public void whenStakeTxnRetriesMultipleTimesCanStillBeReconciled() {

		setupApiClientSendTxnForFail();

		String txnId = addFailedTxn(createStakeTxnRequest());

		when(demoWalletGateway.cancelTxn(any(), any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))//retry 1
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))//retry 2
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))//retry 3
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))//retry 4
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))//retry 5
		.thenAnswer(invocation -> { //Success - retry 6
			TxnCancelRequest txnRequest = invocation.getArgument(0);
			return createOKTxnReceipt(txnRequest.getTxnId(), txnRequest.getGameCode());
		});

		//Fail a few times
		for (int i=0; i<5; i++) {
			reconService.reconcileTxn(txnId);
		}

		//Should have attempted but not managed 5 times
		verify(txnCallbackRepository, times(0)).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());
		verify(demoWalletGateway, times(5)).cancelTxn(any(), any());

		List<String> reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("Txn should still be up for recon", reconTxns.size(), is(1));
		AbstractTxn txn = txnRepository.findById(txnId).get();
		assertThat("transaction reconciliation was tried multiple times", txn.getRetry(), is(5));
		assertThat("transaction is Still pending", txn.getStatus(), is(TxnStatus.PENDING));

		reconService.reconcileTxn(txnId);

		Mockito.verify(exceptionMonitorService, times(0)).monitorException(Mockito.isA(IllegalStateException.class));

		verify(txnCallbackRepository, times(1)).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());
		verify(demoWalletGateway, times(6)).cancelTxn(any(), any());

		reconTxns = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat("no more transactions to recon", reconTxns.size(), is(0));
		txn = txnRepository.findById(txnId).get();
		assertThat("transaction reconciliation was tried for expected retries", txn.getRetry(), is(5));
		assertThat("transaction is Cancelled", txn.getStatus(), is(TxnStatus.CANCELLED));
	}

	private String addFailedTxn(TxnRequest request) {
		try {
			txnService.process(request);
		} catch (ApiException e) {
			assertThat(e.getCode(), is("ApiTimeout"));
		}

		String txnId = request.getTxnId();
		Txn txn = txnRepository.findById(txnId).get();
		txnRepository.save(txn);

		return txnId;
	}

	private void setupApiClientSendTxnForFailAndSuccess() {
		when(demoWalletGateway.processTxn(any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))
		.thenAnswer(invocation -> {
			TxnRequest txnRequest = invocation.getArgument(0);
			return createOKTxnReceipt(txnRequest.getTxnId(), txnRequest.getGameCode());
		});
	}

	private void setupApiClientSendTxnForTimeoutThenPlayerStatusException() {
		when(demoWalletGateway.processTxn(any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"))
		.thenThrow(new PlayerStatusException("Player status ex"));
	}

	private void setupApiClientSendTxnForFail() {
		when(demoWalletGateway.processTxn(any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"));
	}

	private TxnRequest createStakeTxnRequest() {
		return defaultStakeTxnRequestBuilder()
				.playComplete(false)
				.guest(true)
				.mode(Mode.demo)
				.build();
	}

	private TxnRequest createWinTxnRequest() {
		return defaultWinTxnRequestBuilder()
				.guest(true)
				.mode(Mode.demo)
				.build();
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

