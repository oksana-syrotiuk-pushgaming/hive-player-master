/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InternalServerException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.commons.exception.WebAppException;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.exception.*;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.platformidentifier.PlatformIdentifierService;
import io.gsi.hive.platform.player.play.PlayService;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.registry.ReconCounter;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.event.TxnEvent;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.FundType;
import io.micrometer.core.instrument.Tags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TxnServiceTests {

	private TxnService txnService;
	private InternalTxnService internalTxnService;
	private PlatformIdentifierService platformIdentifierService;

	@Mock private TxnRepository txnRepository;
	@Mock private MeshService meshService;
	@Mock private BonusWalletService bonusWalletService;
	@Mock private SessionService sessionService;
	@Mock private PlayService playService;
	@Mock private ReconCounter reconCounter;

	@Before
	public void setup() {
		initMocks(this);
		ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
		Validator validator = vf.getValidator();
		internalTxnService = new InternalTxnService(txnRepository, meshService, null, bonusWalletService, sessionService, null, null);
		txnService = new TxnService(txnRepository, validator, sessionService, internalTxnService, meshService, null, bonusWalletService, playService,platformIdentifierService, reconCounter);

		Session session = new Session();
		session.setAccessToken("testToken");
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);

		when(meshService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(WalletBuilder.aWallet().build());
		when(bonusWalletService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(WalletBuilder.freeroundsWallet().build());
	}

	@Test(expected=IllegalStateException.class)
	public void invalidTxnRequest() {
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
		gameTxn.setIgpCode(null);
		txnService.process(gameTxn);
	}

	@Test
	public void createPersistAndSend() {
		Txn txn = createTxn();
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().txnId("100").build();

		Txn meshTxn = TxnBuilder.txn().build();
		meshTxn.getEvents().add(TxnReceiptBuilder.txnReceipt().build());

		when(txnRepository.saveAndFlush(any(Txn.class))).thenReturn(meshTxn);
		when(txnRepository.findById(txn.getTxnId())).thenReturn(Optional.of(meshTxn));
		when(txnRepository.saveAndFlush(any(Txn.class))).thenReturn(txn);

		when(meshService.sendTxn(txn.getIgpCode(),meshTxn)).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		String txnId = txnService.process(gameTxn).getTxnId();

		assertThat(txnId,is(equalTo(TxnPresets.TXNID)));
	}

	@Test
	public void givenSendingApiError_whenProcessing_thenThrowExceptionAndMetricPublished() {
		Txn txn = createTxn();
		txn.setType(TxnType.WIN);
		TxnRequest gameTxn = defaultWinTxnRequestBuilder().txnType(TxnType.WIN).txnId("100").build();

		Txn meshTxn = TxnBuilder.txn().build();
		meshTxn.getEvents().add(TxnReceiptBuilder.txnReceipt().build());

		when(txnRepository.findById(txn.getTxnId())).thenReturn(Optional.of(meshTxn));
		when(txnRepository.saveAndFlush(any(Txn.class))).thenReturn(txn);

		when(meshService.sendTxn(txn.getIgpCode(),meshTxn)).thenThrow(new ApiKnownException("",""));

		assertThatThrownBy(() -> txnService.process(gameTxn).getTxnId())
				.isInstanceOf(ApiKnownException.class);

		verify(txnRepository).findById(txn.getTxnId());
		verify(txnRepository).findById(txn.getTxnId());
		verify(txnRepository).saveAndFlush(txn);
		verify(meshService).sendTxn(txn.getIgpCode(),meshTxn);
		verify(reconCounter).increment(Tags.of("Method","send","Error", "api_exception"));
	}

	@Test
	public void givenSendingWebAppError_whenProcessing_thenThrowExceptionAndMetricPublished() {
		Txn txn = createTxn();
		txn.setType(TxnType.WIN);
		TxnRequest gameTxn = defaultWinTxnRequestBuilder().txnType(TxnType.WIN).txnId("100").build();

		Txn meshTxn = TxnBuilder.txn().build();
		meshTxn.getEvents().add(TxnReceiptBuilder.txnReceipt().build());

		when(txnRepository.findById(txn.getTxnId())).thenReturn(Optional.of(meshTxn));
		when(txnRepository.saveAndFlush(any(Txn.class))).thenReturn(txn);

		when(meshService.sendTxn(txn.getIgpCode(),meshTxn)).thenThrow(new BadRequestException(""));

		assertThatThrownBy(() -> txnService.process(gameTxn).getTxnId())
				.isInstanceOf(WebAppException.class);

		verify(txnRepository).findById(txn.getTxnId());
		verify(txnRepository).findById(txn.getTxnId());
		verify(txnRepository).saveAndFlush(txn);
		verify(meshService).sendTxn(txn.getIgpCode(),meshTxn);
		verify(reconCounter).increment(Tags.of("Method","send","Error", "web_app_exception"));

	}

	@Test
	public void idempotencyEnforcedOkTxn()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.OK);

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
		String expectedTxnId = txn.getTxnId();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = txnService.process(gameTxn).getTxnId();

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should retrieve new wallet
		verify(meshService).getWallet(gameTxn.getIgpCode(), gameTxn.getPlayerId(), gameTxn.getGameCode(), txn.getAccessToken());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		assertThat(txnId,is(equalTo(expectedTxnId)));
	}

	@Test
	public void givenTxnWinPending_whenProcess_thenProcessImmediately() {
		Txn txn = createTxn();
		txn.setType(TxnType.WIN);
		txn.setMode(Mode.real);
		txn.setStatus(TxnStatus.PENDING);

		TxnRequest gameTxn = defaultWinTxnRequestBuilder().txnId("100").build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		var expectedReceipt = TxnReceiptBuilder.txnReceipt().build();
		when(meshService.sendTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
		when(txnRepository.saveAndFlush(any())).thenReturn(new Txn());

		when(txnRepository.saveAndFlush(any())).thenReturn(txn);
		doNothing().when(playService).updateFromTxnReceipt(any(), any());


		var actualReceipt = txnService.process(gameTxn);

		verify(txnRepository).findById(TxnPresets.TXNID);
		verify(txnRepository, times(2)).findById(anyString());
		verify(meshService).sendTxn(any(), any());

		verify(txnRepository).saveAndFlush(txn);
		verify(txnRepository, times(2)).saveAndFlush(any());
		verify(playService).updateFromTxnReceipt(any(), any());

		actualReceipt.setTimestamp(expectedReceipt.getTimestamp());
		assertThat(actualReceipt,equalTo(expectedReceipt));
	}

	@Test
	public void givenTxnStakePending_whenProcess_thenIdempotencyEnforcedErrorResponse()
	{
		Txn txn = createTxn();

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = "";
		boolean exceptionThrown = false;
		try
		{
			txnId = txnService.process(gameTxn).getTxnId();
		} catch(InternalServerException e)
		{
			assertThat(e.getMessage(), is("Txn Pending"));
			exceptionThrown = true;
		}
		assertThat(exceptionThrown, is(true));

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		//No id should be returned
		assertThat(txnId,is(equalTo("")));
	}

	@Test
	public void idempotencyEnforcedCancelledTxn()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.CANCELLED);

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = "";
		boolean exceptionThrown = false;
		try
		{
			txnId = txnService.process(gameTxn).getTxnId();
		} catch(TxnTombstoneException e)
		{
			assertThat(e.getMessage(), is("Txn Previously Cancelled"));
			exceptionThrown = true;
		}
		assertThat(exceptionThrown, is(true));

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		//No id should be returned
		assertThat(txnId,is(equalTo("")));
	}

	@Test
	public void givenReconTxn_whenProcess_thenIdempotencyEnforcedPlayerStatusResponse()
	{
		var exceptionName = UpstreamGameSupportedException.PlayerStatusException.name();
		Txn txn = createTxn();
		txn.setException(exceptionName);
		txn.setStatus(TxnStatus.RECON);

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = "";
		boolean exceptionThrown = false;
		try
		{
			txnId = txnService.process(gameTxn).getTxnId();
		} catch(PlayerStatusException e)
		{
			assertThat(e.getMessage(), isEmptyString());
			exceptionThrown = true;
		}
		assertThat(exceptionThrown, is(true));

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		assertThat("No id should be returned",txnId,is(equalTo("")));
	}

	@Test
	public void givenReconTxnWithUnknowException_whenProcess_thenIdempotencyEnforcedInternalServerResponse()
	{
		Txn txn = createTxn();
		txn.setException("unknown");
		txn.setStatus(TxnStatus.RECON);

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = "";
		boolean exceptionThrown = false;
		try
		{
			txnId = txnService.process(gameTxn).getTxnId();
		} catch(InternalServerException e)
		{
			assertThat(e.getMessage(), is("Txn Previously Failed"));
			exceptionThrown = true;
		}
		assertThat(exceptionThrown, is(true));

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		assertThat("No id should be returned",txnId,is(equalTo("")));
	}

	@Test
	public void givenReconTxnAndNoTxnException_whenProcess_thenIdempotencyEnforcedInternalServerResponse()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.RECON);

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = "";
		boolean exceptionThrown = false;
		try
		{
			txnId = txnService.process(gameTxn).getTxnId();
		} catch(InternalServerException e)
		{
			assertThat(e.getMessage(), is("Txn Currently in Reconciliation"));
			exceptionThrown = true;
		}
		assertThat(exceptionThrown, is(true));

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		assertThat("No id should be returned",txnId,is(equalTo("")));
	}

	@Test
	public void idempotencyEnforcedFailedTxnInsufficientFunds()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.FAILED);
		txn.setException("InsufficientFundsException");

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = "";
		boolean exceptionThrown = false;
		try
		{
			txnId = txnService.process(gameTxn).getTxnId();
		} catch(InsufficientFundsException e)
		{
			assertThat(e.getMessage(), is(""));//Rethrown exceptions have no message
			exceptionThrown = true;
		}
		assertThat(exceptionThrown, is(true));

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		//No id should be returned
		assertThat(txnId,is(equalTo("")));
	}

	@Test
	public void idempotencyEnforcedFailedTxnDownstreamUnsupported()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.FAILED);
		txn.setException("ApiUnknownException");

		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		String txnId = "";
		boolean exceptionThrown = false;
		try
		{
			txnId = txnService.process(gameTxn).getTxnId();
		} catch(InternalServerException e)
		{
			assertThat(e.getMessage(), is("Txn Previously Failed"));//Unknown rethrowns have generic message
			exceptionThrown = true;
		}
		assertThat(exceptionThrown, is(true));

		//Should have retrieved form repo
		verify(txnRepository).findById(TxnPresets.TXNID);

		//Should not save new Txn
		verify(txnRepository, Mockito.times(0)).saveAndFlush(any());

		//Should not do any processing
		verify(meshService, Mockito.times(0)).sendTxn(Mockito.anyString(), any());

		//No id should be returned
		assertThat(txnId,is(equalTo("")));
	}
	
	@Test
	public void idempotencyReturnsWallet()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.OK);
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
		
		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		var receipt = txnService.process(gameTxn);
		assertThat(receipt.getWallet(), notNullValue());
	}
	
	@Test
	public void idempotencyReturnsBonusWalletWhenBonus()
	{
		Txn txn = createBonusTxn();
		txn.setStatus(TxnStatus.OK);
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
		

		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		var receipt = txnService.process(gameTxn);
		var wallet = receipt.getWallet();
		var bonusFunds = wallet.getFunds().stream()
				.filter(fund -> fund.getType() == FundType.FREEROUNDS)
				.findFirst()
				.orElse(null);
				
		assertThat(bonusFunds, notNullValue());
	}

	@Test
	public void idempotencyReturnsNoBonusWalletWhenNoBonus()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.OK);
		TxnRequest gameTxn = defaultStakeTxnRequestBuilder().build();
		
		when(txnRepository.findById(TxnPresets.TXNID)).thenReturn(Optional.of(txn));

		var receipt = txnService.process(gameTxn);
		var wallet = receipt.getWallet();
		var bonusFunds = wallet.getFunds().stream()
				.filter(fund -> fund.getType() == FundType.FREEROUNDS)
				.findFirst()
				.orElse(null);

		assertThat(bonusFunds, nullValue());
		
		verify(bonusWalletService, Mockito.times(0)).getWallet(
				Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString(),  Mockito.anyString());
				
	}

	@Test
	public void externalCancelFailureNotFound()
	{
		boolean errorThrown = false;
		try {
			txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());
		} catch (NotFoundException e) {
			errorThrown = true;
			assertThat(e.getCode(),is("NotFound"));
		}
		assertThat(errorThrown,is(true));
	}

	@Test
	public void externalCancelFailureClawback()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.OK);
		txn.setType(TxnType.WIN);

		when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(txn);

		boolean errorThrown = false;
		try {
			txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());
		} catch (ClawbackNotSupportedException e) {
			errorThrown = true;
			assertThat(e.getCode(),is("ClawbackNotSupported"));
		}
		assertThat(errorThrown,is(true));
	}

	@Test
	public void externalCancelFailureStateFailed()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.FAILED);
		txn.setType(TxnType.STAKE);

		when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(txn);

		TxnReceipt receipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertEquals(TxnPresets.TXNID, receipt.getTxnId());
		assertEquals(TxnStatus.CANCELLED, receipt.getStatus());
	}

	@Test
	public void externalCancelFailureStateCancelled()
	{
		Txn txn = createTxn();
		txn.setStatus(TxnStatus.CANCELLED);
		txn.setType(TxnType.STAKE);

		when(txnRepository.findAndLockByTxnId(TxnPresets.TXNID)).thenReturn(txn);

		TxnReceipt receipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertEquals(TxnPresets.TXNID, receipt.getTxnId());
		assertEquals(TxnStatus.CANCELLED, receipt.getStatus());	}

	private Txn createTxn() {
		return TxnBuilder.txn()
				.withPlayComplete(false)
				.withRoundComplete(false)
				.withPlayerId("player1")
				.withMode(Mode.real)
				.withAmount(new BigDecimal("1.00"))
				.withTxnTs(ZonedDateTime.of(2016,3,15,12,0,0,0,ZoneId.of("UTC")))
				.build();
	}
	
	private Txn createBonusTxn() {
		ArrayList<TxnEvent> requests = new ArrayList<>();
		requests.add(defaultBonusTxnRequestBuilder().build());

		return TxnBuilder.txn()
				.withPlayComplete(false)
				.withRoundComplete(false)
				.withPlayerId("1")
				.withMode(Mode.real)
				.withAmount(new BigDecimal("1.00"))
				.withTxnTs(ZonedDateTime.of(2016,3,15,12,0,0,0,ZoneId.of("UTC")))
				.withTxnEvents(requests)
				.build();
		
	}
}
