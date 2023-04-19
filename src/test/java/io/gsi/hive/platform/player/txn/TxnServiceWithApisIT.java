package io.gsi.hive.platform.player.txn;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.bonus.gateway.BonusWalletGateway;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.mesh.gateway.DefaultMeshGateway;
import io.gsi.hive.platform.player.mesh.txn.*;
import io.gsi.hive.platform.player.persistence.TxnCleardownRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.PlayBuilder;
import io.gsi.hive.platform.player.play.PlayRepository;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.event.TxnEvent;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

//These tests cover txns with more complex event streams - namely first and last uses of funds.
@Sql(statements={PersistenceITBase.CLEAN_DB_SQL,PersistenceITBase.PLAYER_SQL}, executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class TxnServiceWithApisIT extends ApiITBase{

	private JdbcTemplate jdbcTemplate;
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Autowired
	private TxnRepository txnRepository;
	@Autowired
	private TxnCleardownRepository txnCleardownRepository;
	@Autowired
	private TxnService txnService;

	@SpyBean
	private PlayRepository playRepository;

	@MockBean
	private DefaultMeshGateway meshApiGatewayMock;
	@MockBean
	private SessionService sessionServiceMock;
	@MockBean
	private BonusWalletGateway bonusWalletGatewayMock;

	@Before
	public void setup() {
		Session session = new Session();
		session.setAccessToken("testToken");
		when(sessionServiceMock.getSession(Mockito.anyString())).thenReturn(session);
	}

	@After
	public void resetPlayRepositoryMock() {
		Mockito.reset(playRepository);
	}

	/**
	 * Note that previously a zero stake txn was sent to mesh on first bonus fund use, but not anymore
	 */
	@Test
	public void givenStakeTxnWithBonusFund_whenProcessTxn_thenBonusWalletHitAndDataStoredCorrect() {
		Wallet bonusWallet = WalletBuilder.freeroundsWallet()
				.withFunds(List.of(FreeroundsFundBuilder.freeroundsFund().withAwarded(10).withRemaining(9).build()))
				.build();
		TxnRequest stakeRequest = defaultStakeTxnRequestBuilder()
				.playComplete(false)
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();

		when(bonusWalletGatewayMock.processTxn(any()))
				.thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());

		txnService.process(stakeRequest).getTxnId();

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK'"))
				.as("txn added to t_txn table")
				.isEqualTo(1);

		//Not bothered on parameters, important thing is that it is sent
		Mockito.verify(meshApiGatewayMock, never().description("txn not sent to mesh node"))
				.processTxn(any(), any(), any());
		Mockito.verify(bonusWalletGatewayMock, description("txn sent to bonus wallet"))
				.processTxn(any());

		AbstractTxn txn = txnRepository.findById(TxnPresets.TXNID).get();
		assertThat(txn).isNotNull();
		assertThat(txn.getAmount()).as("stored stake amount matches txn request")
				.isEqualByComparingTo(stakeRequest.getAmount());

		Optional<Txn> txnOptional = txnRepository.findById(txn.getTxnId());
		assertThat(txnOptional).as("txn stored").isNotEmpty();
		TxnReceipt bonusReceipt = (TxnReceipt) txnOptional.get().getEvents().get(1);

		//Check that all reciepts are present and properly combined into Player receipt
		List<TxnEvent> events = txn.getEvents();
		TxnReceipt playerReceipt = (TxnReceipt) events.get(events.size()-1);
		assertThat(events).as("txn events has request and receipt").hasSize(2);
		assertThat(playerReceipt.getWallet().getFunds()).as("player receipt has the bonus wallet's funds")
				.containsAll(bonusReceipt.getWallet().getFunds());
		assertThat(playerReceipt.getTxnId())
				.as("play receipt has correct txn id").isEqualTo(TxnPresets.TXNID);
	}


	/**
	 * all plays on freerounds should result in RGS_FREEROUND_WIN txns making it to mesh
	 */
	@Test
	@Transactional
	public void givenWinTransactionAndUnfinishedBonusFund_whenProcessTxn_thenPlayerReceiptCorrect() {
		Wallet bonusWallet = WalletBuilder.freeroundsWallet()
				.withFunds(List.of(FreeroundsFundBuilder.freeroundsFund()
						.withAwarded(10)
						.withRemaining(5)
						.withCumulativeWin(BigDecimal.valueOf(50))
						.build()))
				.build();
		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();
		MeshGameTxnAction expectedMeshFreeroundWinTxnAction = new MeshGameTxnActionBuilder()
				.withRgsActionId("award1")
				.withType(MeshGameTxnActionType.RGS_FREEROUND_WIN)
				.withAmount(winRequest.getAmount())
				.withJackpotAmount(null)
				.get();
		MeshGameTxn expectedMeshFreeroundWinTxn = new MeshGameTxnBuilder()
				.withActions(expectedMeshFreeroundWinTxnAction)
				.withPlayComplete(true)
				.withRgsTxnId(winRequest.getTxnId())
				.withRgsGameId(winRequest.getGameCode())
				.withRgsPlayId(winRequest.getPlayId())
				.withRgsRoundId(winRequest.getRoundId())
				.withRoundComplete(winRequest.getRoundComplete())
				.withExtraInfo(null)
				.get();
		Play playCreatedUponStake = PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build();

		when(bonusWalletGatewayMock.processTxn(any())).thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());
		when(meshApiGatewayMock.processTxn(any(), any(), any())).thenReturn(new MeshGameTxnStatusBuilder().get());
		when(playRepository.findAndLockByPlayId(winRequest.getPlayId())).thenReturn(Optional.of(playCreatedUponStake));

		txnService.process(winRequest);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'OK'")).isEqualTo(1);

		Mockito.verify(meshApiGatewayMock).processTxn(any(), eq(expectedMeshFreeroundWinTxn), any());
		Mockito.verify(bonusWalletGatewayMock).processTxn(any());
		Mockito.verify(bonusWalletGatewayMock, never()).closeFund(WalletPresets.BONUSFUNDID);

		AbstractTxn txn = txnRepository.findById(TxnPresets.TXNID).get();
		assertThat(txn).isNotNull();
		assertThat(txn.getAmount().compareTo(winRequest.getAmount())).isEqualTo(0);

		List<TxnEvent> events = txn.getEvents();
		assertThat(events.size()).isEqualTo(2);//request, receipt for player

		//Check that all receipts are present and properly combined into Player receipt
		TxnReceipt playerReceipt = (TxnReceipt) events.get(events.size()-1);

		//Check the txn was stored for RGS_FREEROUND_WIN
		Txn freeroundTxn = txnRepository.findById(txn.getTxnId()).get();
		TxnReceipt txnReceipt = (TxnReceipt) freeroundTxn.getEvents().get(1);

		assertThat(playerReceipt.getWallet().getFunds()).containsAll(txnReceipt.getWallet().getFunds());
		assertThat(playerReceipt.getWallet().getBalance()).isEqualByComparingTo(txnReceipt.getWallet().getBalance());
		assertThat(playerReceipt.getTxnId()).isEqualTo(TxnPresets.TXNID);
	}


	/**
	 * TODO need to test the RGS_FREEROUND_WIN mesh txn as well as the RGS_FREEROUND_CLEARDOWN txn
	 */
	@Test
	@Transactional
	public void givenWinTxnAndFinishedFreeroundFund_whenProcessTxn_thenPlayerCleardownCorrect() {
		Wallet bonusWallet = WalletBuilder.freeroundsWallet()
				.withFunds(new ArrayList<>(Arrays.asList(FreeroundsFundBuilder.freeroundsFund()
						.withAwarded(10)
						.withRemaining(0)
						.withCumulativeWin(BigDecimal.valueOf(100))
						.build()))).build();
		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();
		MeshGameTxnAction expectedMeshFreeroundWinTxnAction = new MeshGameTxnActionBuilder()
				.withRgsActionId("award1")
				.withType(MeshGameTxnActionType.RGS_FREEROUND_WIN)
				.withAmount(winRequest.getAmount())
				.withJackpotAmount(null)
				.get();
		MeshGameTxn expectedMeshFreeroundWinTxn = new MeshGameTxnBuilder()
				.withActions(expectedMeshFreeroundWinTxnAction)
				.withPlayComplete(false)
				.withRgsTxnId(winRequest.getTxnId())
				.withRgsGameId(winRequest.getGameCode())
				.withRgsPlayId(winRequest.getPlayId())
				.withRgsRoundId(winRequest.getRoundId())
				.withRoundComplete(winRequest.getRoundComplete())
				.withExtraInfo(null)
				.get();
		MeshGameTxnAction expectedMeshFreeroundCleardownTxnAction = new MeshGameTxnActionBuilder()
				.withRgsActionId("award1")
				.withType(MeshGameTxnActionType.RGS_FREEROUND_CLEARDOWN)
				.withAmount(BigDecimal.valueOf(100))
				.withJackpotAmount(null)
				.get();
		MeshGameTxn expectedMeshFreeroundCleardownTxn = new MeshGameTxnBuilder()
				.withActions(expectedMeshFreeroundCleardownTxnAction)
				.withPlayComplete(true)
				.withRgsTxnId("FRCLR-"+ TxnPresets.PLATFORMID +"-" + WalletPresets.BONUSFUNDID)
				.withRgsGameId(winRequest.getGameCode())
				.withRgsPlayId(winRequest.getPlayId())
				.withRgsRoundId(winRequest.getRoundId())
				.withRoundComplete(winRequest.getRoundComplete())
				.withExtraInfo(null)
				.get();
		Play playCreatedUponStake = PlayBuilder.play().withStatus(PlayStatus.ACTIVE).build();

		when(bonusWalletGatewayMock.processTxn(any())).thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());
		when(meshApiGatewayMock.processTxn(any(), any(), any())).thenReturn(new MeshGameTxnStatusBuilder().get());
		when(playRepository.findAndLockByPlayId(winRequest.getPlayId())).thenReturn(Optional.of(playCreatedUponStake));

		txnService.process(winRequest);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'OK'")).isEqualTo(1);

		Mockito.verify(meshApiGatewayMock).processTxn(any(), eq(expectedMeshFreeroundCleardownTxn), any());
		Mockito.verify(meshApiGatewayMock).processTxn(any(), eq(expectedMeshFreeroundWinTxn), any());
		Mockito.verify(bonusWalletGatewayMock).processTxn(any());
		Mockito.verify(bonusWalletGatewayMock).closeFund(WalletPresets.BONUSFUNDID);

		AbstractTxn txn = txnRepository.findById(TxnPresets.TXNID).get();
		assertThat(txn).isNotNull();
		assertThat(txn.getAmount().compareTo(winRequest.getAmount())).isEqualTo(0);

		List<TxnEvent> events = txn.getEvents();
		assertThat(events.size()).isEqualTo(3);//request, receipt for player, cleardown

		//Check that all receipts are present and properly combined into Player receipt
		TxnReceipt playerReceipt = (TxnReceipt) events.get(events.size()-1);

		//Check the internal mesh txn was stored for RGS_FREEROUND_CLEARDOWN
		TxnCleardown cleardownTxn = txnCleardownRepository.findById(TxnPresets.TXNID).get();

		assertThat(cleardownTxn).isNotNull();
		assertThat(cleardownTxn.getCleardownTxnId()).isEqualTo("FRCLR-" + TxnPresets.PLATFORMID + "-" + WalletPresets.BONUSFUNDID);

		TxnReceipt txnReceipt = (TxnReceipt) txn.getEvents().get(2);

		assertThat(playerReceipt.getWallet().getFunds()).containsAll(txnReceipt.getWallet().getFunds());
		assertThat(playerReceipt.getWallet().getBalance()).isEqualByComparingTo(txnReceipt.getWallet().getBalance());
		assertThat(playerReceipt.getTxnId()).isEqualTo(TxnPresets.TXNID);
	}

}
