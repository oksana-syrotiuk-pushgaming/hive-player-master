package io.gsi.hive.platform.player.recon.game;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.demo.gateway.DemoWalletGateway;
import io.gsi.hive.platform.player.game.Game;
import io.gsi.hive.platform.player.game.GameService;
import io.gsi.hive.platform.player.mesh.gateway.DefaultMeshGateway;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuth;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuthBuilder;
import io.gsi.hive.platform.player.mesh.presets.MeshPlayerIdPresets;
import io.gsi.hive.platform.player.mesh.txn.*;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus.Status;
import io.gsi.hive.platform.player.persistence.TxnCallbackRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.recon.ReconTxnService;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;

import static io.gsi.hive.platform.player.txn.BonusFundDetailsPresets.defaultOperatorBonusFundDetails;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL,PersistenceITBase.PLAYER_SQL,PersistenceITBase.GUEST_PLAYER_SQL},
		executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class IntegrationAndReconIT extends ApiITBase{
	@MockBean
	private TxnCallbackRepository txnCallbackRepository;
	@MockBean
	private DemoWalletGateway demoWalletGateway;
	@MockBean
	private SessionService sessionService;
	@MockBean
	private GameService gameService;
	@MockBean
	private DefaultMeshGateway defaultMeshGateway;

	@Autowired
	private ReconTxnService txnReconService;
	@Autowired
	private TxnRepository txnRepository;
	@Autowired
	private PlayRepository playRepository;
	@Autowired
	private ReconTxnIntegrationService reconIntegrationService;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this.getClass());
		//We're only interested in the Id of the game from the lookup
		Game gameMock = Mockito.mock(Game.class);
		Mockito.when(gameService.getGame(GamePresets.CODE)).thenReturn(gameMock);

		Session session = new Session();
		session.setAccessToken("testToken");
		Mockito.when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
	}

	@Test
	public void integrateAndReconStake() {
		MeshGameTxnCancel expectedCancelTxn = MeshGameTxnCancel.builder()
				.playerId(MeshPlayerIdPresets.DEFAULT)
				.rgsTxnCancelId(null)
				.playComplete(true)
				.roundComplete(true)
				.reason(null)
				.rgsGameId("testGame")
				.currency("GBP")
				.rgsRoundId("1000-10")
				.rgsPlayId("1000-10")
				.amount(new BigDecimal("20.00").setScale(2, RoundingMode.UNNECESSARY))
				.build();
		integrateAndReconStake(defaultStakeTxnRequestBuilder().build(), expectedCancelTxn);
	}

	@Test
	public void integrateAndReconOperatorFreeroundsStake() {
		OperatorBonusFundDetails operatorBonusFundDetails = defaultOperatorBonusFundDetails().build();
		TxnRequest operatorFreeroundsStakeRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(operatorBonusFundDetails)
				.build();

		MeshGameTxnCancel expectedCancelTxn = MeshGameTxnCancel.builder()
				.playerId(MeshPlayerIdPresets.DEFAULT)
				.rgsTxnCancelId(null)
				.playComplete(true)
				.roundComplete(true)
				.reason(null)
				.rgsGameId("testGame")
				.currency("GBP")
				.rgsRoundId("1000-10")
				.rgsPlayId("1000-10")
				.amount(new BigDecimal("20.00").setScale(2, RoundingMode.UNNECESSARY))
				.extraInfo(Map.of("bonusFundDetails", operatorBonusFundDetails))
				.build();
		integrateAndReconStake(operatorFreeroundsStakeRequest, expectedCancelTxn);
	}

	private void integrateAndReconStake(final TxnRequest stakeRequest, final MeshGameTxnCancel expectedCancelTxn) {
		reconIntegrationService.integrateGameTxn(stakeRequest);

		Mockito.when(defaultMeshGateway.cancelTxn(any(), any(), any(), any())).thenReturn(new MeshGameTxnStatusBuilder().withStatus(Status.CANCELLED).get());
		txnReconService.reconcileTxnAndPlay(TxnPresets.TXNID);

		MeshPlayerAuth expectedMeshAuth = new MeshPlayerAuthBuilder().withToken(AuthorizationPresets.ACCESSTOKEN).get();
		Mockito.verify(defaultMeshGateway).cancelTxn(TxnPresets.TXNID, expectedCancelTxn, IgpPresets.IGPCODE_IGUANA, expectedMeshAuth);
		Mockito.verify(txnCallbackRepository, times(1)).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		List<String> reconTxnKeys = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat(reconTxnKeys.size()).isEqualTo(0);

		assertThat(txnRepository.findById(TxnPresets.TXNID)).isNotNull();
	}

	@Test
	public void integrateAndReconDemoStake() {
		TxnRequest txnRequest = defaultStakeTxnRequestBuilder()
				.guest(true)
				.mode(Mode.demo)
				.build();
		reconIntegrationService.integrateGameTxn(txnRequest);

		Mockito.when(demoWalletGateway.cancelTxn(any(), any())).thenReturn(TxnReceiptBuilder.txnReceipt().withStatus(TxnStatus.CANCELLED).build());
		txnReconService.reconcileTxnAndPlay(TxnPresets.TXNID);

		TxnCancelRequest expectedCancelTxn = TxnCancelRequestBuilder.txnCancelRequest()
				.withPlayComplete(true).withRoundComplete(true)
				.build();

		Mockito.verify(demoWalletGateway).cancelTxn(Mockito.refEq(expectedCancelTxn, "timestamp"), Mockito.eq(TxnPresets.TXNID));
		Mockito.verify(txnCallbackRepository, times(1)).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		List<String> reconTxnKeys = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat(reconTxnKeys.size()).isEqualTo(0);

		assertThat(txnRepository.findById(TxnPresets.TXNID)).isNotNull();
	}


	@Test
	public void integrateAndReconWin() {
		TxnRequest txnRequest = defaultWinTxnRequestBuilder()
				.extraInfo(new LinkedHashMap<>())
				.playComplete(false)
				.jackpotAmount(null)
				.build();
		MeshGameTxnAction meshGameWinTxnAction = new MeshGameTxnActionBuilder()
				.withRgsActionId(TxnPresets.TXNID)
				.withType(MeshGameTxnActionType.WIN)
				.get();
		integrateAndReconWin(txnRequest, meshGameWinTxnAction);
	}

	@Test
	public void integrateAndReconOperatorFreeroundsWin() {
		TxnRequest txnRequest = defaultWinTxnRequestBuilder()
				.extraInfo(new LinkedHashMap<>())
				.playComplete(false)
				.jackpotAmount(null)
				.bonusFundDetails(defaultOperatorBonusFundDetails().build())
				.build();
		MeshOperatorFreeroundGameTxnAction meshGameOperatorFreeroundsWinAction =
				new MeshOperatorFreeroundGameTxnAction(
					MeshGameTxnActionType.OPERATOR_FREEROUND_WIN,
					"1000-1",
					MonetaryPresets.BDAMOUNT,
					null,
					WalletPresets.BONUS_ID,
					WalletPresets.AWARD_ID,
					WalletPresets.FREEROUNDS_REMAINING,
					WalletPresets.EXTRA_INFO);
		integrateAndReconWin(txnRequest, meshGameOperatorFreeroundsWinAction);
	}

	private void integrateAndReconWin(final TxnRequest txnRequest, final MeshGameTxnAction... expectedTxnActions) {
		Play play = PlayBuilder.play()
				.withPlayId(txnRequest.getPlayId())
				.withStatus(PlayStatus.ACTIVE).build();
		playRepository.saveAndFlush(play);

		reconIntegrationService.integrateGameTxn(txnRequest);

		Mockito.when(defaultMeshGateway.processTxn(any(),  any(), any())).thenReturn(new MeshGameTxnStatusBuilder().get());

		txnReconService.reconcileTxnAndPlay(TxnPresets.TXNID);

		Mockito.verify(defaultMeshGateway).processTxn(
				new MeshPlayerAuthBuilder().withToken(AuthorizationPresets.ACCESSTOKEN).get(),
				new MeshGameTxnBuilder()
				.withRgsTxnId(TxnPresets.TXNID)
				.withRgsGameId(GamePresets.CODE)
				.withRgsPlayId(TxnPresets.PLAYID)
				.withRgsRoundId(TxnPresets.ROUNDID)
				.withRoundComplete(true)
				.withExtraInfo(new LinkedHashMap<>())
				.withActions(expectedTxnActions)
				.get(),
				IgpPresets.IGPCODE_IGUANA);
		Mockito.verify(txnCallbackRepository, times(1)).saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		List<String> reconTxnKeys = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat(reconTxnKeys.size()).isEqualTo(0);

		assertThat(txnRepository.findById(TxnPresets.TXNID)).isNotNull();
	}

	@Test
	public void givenDemoWinIntegrated_whenReconcileTxnAndPlay_thenTxnPresentAndNotInRecon() {
		TxnRequest txnRequest = defaultStakeTxnRequestBuilder()
				.extraInfo(new LinkedHashMap<>())
				.jackpotAmount(null)
				.guest(true)
				.mode(Mode.demo)
				.txnType(TxnType.WIN)
				.build();
		TxnRequest expectedOutwardsTxn = defaultStakeTxnRequestBuilder()
				.mode(Mode.demo)
				.guest(true)
				.txnType(TxnType.WIN)
				.jackpotAmount(null)
				.extraInfo(Collections.emptyMap())
				.build();
		Play play = PlayBuilder.play()
				.withPlayId(txnRequest.getPlayId())
				.withStatus(PlayStatus.ACTIVE).build();
		playRepository.saveAndFlush(play);

		reconIntegrationService.integrateGameTxn(txnRequest);

		Mockito.when(demoWalletGateway.processTxn(any()))
				.thenReturn(TxnReceiptBuilder.txnReceipt().withStatus(TxnStatus.OK).build());

		txnReconService.reconcileTxnAndPlay(TxnPresets.TXNID);

		Mockito.verify(demoWalletGateway).processTxn(Mockito.refEq(expectedOutwardsTxn, "timestamp"));
		Mockito.verify(txnCallbackRepository, times(1))
				.saveToCallbackQueue(Mockito.eq(TxnPresets.TXNID), any(), any());

		List<String> reconTxnKeys = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat(reconTxnKeys.size()).isZero();
		assertThat(txnRepository.findById(TxnPresets.TXNID)).isNotNull();
	}

	/**
	 * We are now catching WebAppExceptions in txnService:cancel().
	 * This results in a null token being caught as a known exception and entering manual recon.
	 */
	@Test
	public void integrateAndReconStakeWithNullToken() {
		//The token in the integrated txn will be null
		Session session = new Session();
		Mockito.when(sessionService.getSession(Mockito.anyString())).thenReturn(session);

		reconIntegrationService.integrateGameTxn(defaultStakeTxnRequestBuilder().build());

		assertThatThrownBy(() -> txnReconService.reconcileTxnAndPlay(TxnPresets.TXNID)).isInstanceOf(AuthorizationException.class);

		Mockito.verify(defaultMeshGateway, Mockito.times(0)).cancelTxn(any(), any(), any(), any());
		Mockito.verify(txnCallbackRepository, times(0)).saveToCallbackQueue(any(), any(), any());

		//Should no longer be up for recon - known exception is caught and set to manual recon.
		List<String> reconTxnKeys = txnReconService.getTxnsForRecon(ZonedDateTime.now(), 10);
		assertThat(reconTxnKeys.size()).isEqualTo(0);

		Optional<Txn> actualTxn = txnRepository.findById(TxnPresets.TXNID);
		assertThat(actualTxn).isPresent();
		assertThat(actualTxn.get().getStatus()).isEqualTo(TxnStatus.RECON);
		//getOrCreateInternalMeshTxn() is no longer rolled back, resulting in a meshTxn saved.
		assertThat(txnRepository.findById(TxnPresets.TXNID).isEmpty()).isFalse();
	}
}
