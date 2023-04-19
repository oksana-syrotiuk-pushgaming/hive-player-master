/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InternalServerException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelRequestBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.demo.DemoWalletService;
import io.gsi.hive.platform.player.exception.*;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.play.PlayService;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnEvent;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static io.gsi.commons.test.string.StringUtils.generateRandomString;
import static io.gsi.hive.platform.player.builders.WalletBuilder.operatorFreeroundsWallet;
import static io.gsi.hive.platform.player.txn.BonusFundDetailsPresets.defaultOperatorBonusFundDetails;
import static io.gsi.hive.platform.player.txn.OperatorFreeroundsFundPresets.baseOperatorFreeroundsFund;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL},
		executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements={PersistenceITBase.CLEAN_DB_SQL}, executionPhase= Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class TxnServiceIT extends ApiITBase {

	private JdbcTemplate jdbcTemplate;
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	//Dont mock InternalTxnService
	@MockBean private MeshService meshService;
	@MockBean private DemoWalletService demoWalletService;
	@MockBean private SessionService sessionService;
	@MockBean private BonusWalletService bonusWalletService;
	@MockBean private PlayService playService;

	@Autowired
	private TxnService txnService;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this.getClass());

		Session session = new Session();
		session.setAccessToken("testToken");
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
	}

	@Test
	public void stakeOk() {
		when(meshService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
		txnService.process(defaultStakeTxnRequestBuilder().build());
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK'")).isEqualTo(1);
	}

	@Test
	public void stakeOkWithBonusFund() {
		Wallet bonusWallet = WalletBuilder.freeroundsWallet().build();
		when(bonusWalletService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());

		TxnRequest stakeRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();

		TxnReceipt receipt = txnService.process(stakeRequest);

		assertThat(receipt.getWallet().getFunds()).contains(FreeroundsFundBuilder.freeroundsFund().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK'")).isEqualTo(1);
	}

	@Test
	public void stakeOkWithOperatorFreeroundBonusFund() {
		OperatorBonusFundDetails operatorBonusFundDetails = defaultOperatorBonusFundDetails().build();
		TxnRequest stakeRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(operatorBonusFundDetails)
				.build();

		txnWithOperatorFreeroundBonusFund(stakeRequest, operatorBonusFundDetails, TxnType.OPFRSTK);
	}

	@Test
	public void winOkWithOperatorFreeroundBonusFund() {
		OperatorBonusFundDetails operatorBonusFundDetails = defaultOperatorBonusFundDetails().build();
		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(operatorBonusFundDetails)
				.build();
		txnWithOperatorFreeroundBonusFund(winRequest, operatorBonusFundDetails, TxnType.OPFRWIN);
	}

	private void txnWithOperatorFreeroundBonusFund(TxnRequest txnRequest,
			OperatorBonusFundDetails operatorBonusFundDetails, TxnType txnType) {
		Wallet operatorFreeroundsBonusWallet = operatorFreeroundsWallet().build();

		when(meshService.sendOperatorFreeroundsTxn(eq(txnRequest.getIgpCode()), any(), eq(operatorBonusFundDetails)))
				.thenReturn(TxnReceiptBuilder.txnReceipt()
						.withWallet(operatorFreeroundsBonusWallet)
						.build());

		TxnReceipt receipt = txnService.process(txnRequest);

		assertThat(receipt.getWallet().getFunds()).contains(baseOperatorFreeroundsFund().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = '" + txnType.name() + "' and status = 'OK'")).isEqualTo(1);
	}

	@Test
	public void stakeOkDemo() {
		when(demoWalletService.sendTxn(any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		TxnRequest demoStake = defaultStakeTxnRequestBuilder().build();
		demoStake.setMode(Mode.demo);
		txnService.process(demoStake);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK'")).isEqualTo(1);
	}

	@Test
	public void stakeFailsWithRuntimeException_noRollBack() {
		when(meshService.sendTxn(any(),any()))
				.thenThrow(new RuntimeException("test_runtime"));
		boolean errorThrown = false;
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build());
		} catch (RuntimeException e) {
			errorThrown = true;
			assertThat(e.getMessage()).isEqualTo("test_runtime");
		}
		assertThat(errorThrown).isEqualTo(true);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID
						+ "' and type = 'STAKE' and status = 'PENDING';")).isEqualTo(1);

	}

	@Test
	public void stakeFailsWithInternalServerException_fromMesh_noRollBack() {
		when(meshService.sendTxn(any(),any()))
				.thenThrow(new InternalServerException("message") {
				});
		boolean errorThrown = false;
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build());
		} catch (InternalServerException e) {
			errorThrown = true;
			assertThat(e.getCode()).isEqualTo("InternalServer");
		}
		assertThat(errorThrown).isEqualTo(true);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID
						+ "' and type = 'STAKE' and status = 'PENDING';")).isEqualTo(1);

	}

	@Test
	public void stakeFailsWithRestClientException_fromMesh_noRollBack() {
		when(meshService.sendTxn(any(),any()))
				.thenThrow(new RestClientException("message") {
				});
		boolean errorThrown = false;
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build());
		} catch (RestClientException e) {
			errorThrown = true;
		}
		assertThat(errorThrown).isEqualTo(true);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID
						+ "' and type = 'STAKE' and status = 'PENDING';")).isEqualTo(1);

	}

	@Test
	public void stakeFailsWithKnownException() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new ApiKnownException("API_INSUFFICIENT_FUNDS","message"));
		boolean errorThrown = false;
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build());
		} catch (ApiException e) {
			errorThrown = true;
			assertThat(e.getCode()).isEqualTo("API_INSUFFICIENT_FUNDS");
		}
		assertThat(errorThrown).isEqualTo(true);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'FAILED'" +
				" and exception = 'API_INSUFFICIENT_FUNDS';")).isEqualTo(1);
	}

	@Test
	public void whenStakeFailsWithKnownCommonsWebAppException_thenPersistAsFailed() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new AuthorizationException("AuthorizationTest"));

		var stakeTxnRequest = defaultStakeTxnRequestBuilder().build();

		assertThatThrownBy(() -> txnService.process(stakeTxnRequest))
		.isInstanceOf(AuthorizationException.class)
		.hasFieldOrPropertyWithValue("code", "Authorization");

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'FAILED'" +
				" and exception = 'Authorization';")).isOne();
	}

	@Test
	public void whenStakeFailsWithUnknownCommonsWebAppException_thenPersistAsPending() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new InternalServerException("Unknown"));

		var winTxnRequest = defaultStakeTxnRequestBuilder().build();

		assertThatThrownBy(() -> txnService.process(winTxnRequest))
		.isInstanceOf(InternalServerException.class)
		.hasMessage("Unknown");

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'PENDING'" +
				" and exception = 'InternalServer';")).isOne();
	}

	@Test
	public void whenWinFailsWithKnownCommonsWebAppException_thenPersistAsRecon() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new AuthorizationException("AuthorizationTest"));

		var winTxnRequest = defaultWinTxnRequestBuilder().build();

		assertThatThrownBy(() -> txnService.process(winTxnRequest))
		.isInstanceOf(AuthorizationException.class)
		.hasFieldOrPropertyWithValue("code", "Authorization");

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'RECON'" +
				" and exception = 'Authorization';")).isOne();
	}

	@Test
	public void givenWinTxnBadRequestException_whenProcess_thenPersistAsRecon() {
		when(meshService.sendTxn(any(),any()))
				.thenThrow(new BadRequestException("BadRequestException"));

		var winTxnRequest = defaultWinTxnRequestBuilder().build();

		assertThatThrownBy(() -> txnService.process(winTxnRequest))
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("code", "BadRequest");

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'RECON'" +
						" and exception = 'BadRequest';"))
				.as("win transaction status set to RECON")
				.isOne();
	}

	@Test
	public void givenStakeTxnBadRequestException_whenProcess_thenPersistAsFailed() {
		when(meshService.sendTxn(any(),any()))
				.thenThrow(new BadRequestException(""));

		var stakeTxnRequest = defaultStakeTxnRequestBuilder().build();

		assertThatThrownBy(() -> txnService.process(stakeTxnRequest))
				.as("GSI BadRequestException has been thrown")
				.isInstanceOf(BadRequestException.class)
				.hasFieldOrPropertyWithValue("code", "BadRequest");

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'FAILED'" +
						" and exception = 'BadRequest';"))
				.as("stake transaction status set to failed")
				.isOne();
	}

	@Test
	public void givenWinTxnAndApiUnexpectedException_whenProcess_thenTxnIsPending() {
		when(meshService.sendTxn(any(),any()))
				.thenThrow(new ApiUnexpectedException(""));

		var winTxnRequest = defaultWinTxnRequestBuilder().build();

		assertThatThrownBy(() -> txnService.process(winTxnRequest))
				.as("apiUnexpectedException has been thrown")
				.isInstanceOf(ApiUnexpectedException.class)
				.hasFieldOrPropertyWithValue("ERROR_CODE", "ApiUnexpected");

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'PENDING'" +
						" and exception = 'ApiUnexpected';"))
				.as("transaction is left in pending state")
				.isOne();
	}

	@Test
	public void whenWinFailsWithUnknownCommonsWebAppException_thenPersistAsPending() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new InternalServerException("Unknown"));

		var winTxnRequest = defaultWinTxnRequestBuilder().build();

		assertThatThrownBy(() -> txnService.process(winTxnRequest))
		.isInstanceOf(InternalServerException.class)
		.hasMessage("Unknown");

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'PENDING'" +
				" and exception = 'InternalServer';")).isOne();
	}

	@Test
	public void stakeFailsWithUnknownException() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"));

		boolean errorThrown = false;
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build());
		} catch (ApiException e) {
			errorThrown = true;
			assertThat(e.getCode()).isEqualTo("ApiTimeout");
		}
		assertThat(errorThrown).isEqualTo(true);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'PENDING'")).isEqualTo(1);
	}

	@Test
	public void stakeFailsWithRAE() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new ResourceAccessException("API_TIMEOUT"));

		boolean errorThrown = false;
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build());
		} catch (ResourceAccessException e) {
			errorThrown = true;
		}
		assertThat(errorThrown).isEqualTo(true);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'PENDING'")).isEqualTo(1);
	}

	@Test
	public void winOk() {
		when(meshService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		txnService.process(defaultWinTxnRequestBuilder().build());
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'OK'")).isEqualTo(1);
	}

	@Test
	public void winOkWithExpiredSession() {
		Session session = new Session();
		session.setAccessToken("testToken");
		session.setLastAccessedTime(1L);
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
		when(meshService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		txnService.process(defaultWinTxnRequestBuilder().build());
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'OK'")).isEqualTo(1);
		Mockito.verify(sessionService, times(1)).getSession(SessionPresets.SESSIONID);

	}

	@Test
	public void winOkWithBonusFund() {
		Wallet bonusWallet = WalletBuilder.freeroundsWallet().build();
		when(bonusWalletService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());
		//we expect a call to mesh RGS_FREEROUND_WIN
		when(meshService.sendFreeroundsWinTxn(any(), any(),any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();

		txnService.process(winRequest);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'OK'")).isEqualTo(1);

		Mockito.verify(meshService).sendFreeroundsWinTxn(any(), any(), any(),any());
		Mockito.verify(bonusWalletService).sendTxn(any(), any());
	}

	@Test
	public void winOkWithBonusFundAndExpiredSession() {
		Session session = new Session();
		session.setAccessToken("testToken");
		session.setLastAccessedTime(1L);
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
		Wallet bonusWallet = WalletBuilder.freeroundsWallet().build();
		when(bonusWalletService.sendTxn(any(),any()))
				.thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());
		when(meshService.sendFreeroundsWinTxn(any(), any(),any(),any()))
				.thenReturn(TxnReceiptBuilder.txnReceipt().build());

		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();

		txnService.process(winRequest);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'OK'")).isEqualTo(1);
		Mockito.verify(sessionService, times(1)).getSession(SessionPresets.SESSIONID);

	}

	@Test
	public void winOkWithFinishedBonusFund() {
		Wallet bonusWallet = WalletBuilder.freeroundsWallet()
				.withFunds(List.of(FreeroundsFundBuilder.freeroundsFund().withRemaining(0).build())).build();

		when(bonusWalletService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());
		when(meshService.sendFreeroundsCleardownTxn(any(), any(),any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
		when(meshService.sendFreeroundsWinTxn(any(), any(),any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();

		txnService.process(winRequest);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'OK'")).isEqualTo(1);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn_cleardown",
				"txn_id = '" + TxnPresets.TXNID + "' AND cleardown_txn_id = 'FRCLR-"
						+ TxnPresets.PLATFORMID + "-" + WalletPresets.BONUSFUNDID + "'")).isEqualTo(1);

		//Not bothered on parameters, important thing is that it is sent
		Mockito.verify(meshService).sendFreeroundsCleardownTxn(any(), any(), any(),any());
		Mockito.verify(meshService).sendFreeroundsWinTxn(any(), any(), any(),any());
	}


	@Test
	public void winFailsWithKnownException() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new ApiKnownException("API_PLAYER_SUSPENDED","message"));
		boolean errorThrown = false;
		try {
			txnService.process(defaultWinTxnRequestBuilder().build());
		} catch (ApiException e) {
			errorThrown = true;
			assertThat(e.getCode()).isEqualTo("API_PLAYER_SUSPENDED");
		}
		assertThat(errorThrown).isEqualTo(true);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'RECON'")).isEqualTo(1);
	}

	@Test
	public void winFailsWithUnknownException() {
		when(meshService.sendTxn(any(),any()))
		.thenThrow(new ApiTimeoutException("API_TIMEOUT"));
		boolean errorThrown = false;
		try {
			txnService.process(defaultWinTxnRequestBuilder().build());
		} catch (ApiException e) {
			errorThrown = true;
			assertThat(e.getCode()).isEqualTo("ApiTimeout");
		}
		assertThat(errorThrown).isTrue();
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID+ "' and type = 'WIN' and status = 'PENDING'")).isEqualTo(1);
	}

	@Test
	@Sql(statements={PersistenceITBase.PLAYER_SQL, PersistenceITBase.STAKE_TXN_SQL})
	public void cancelOk()
	{
		Txn txn = TxnBuilder.txn().withMode(Mode.real).build();
		cancelOk(txn);

		ArgumentCaptor<Txn> txnCaptor = ArgumentCaptor.forClass(Txn.class);
		Mockito.verify(meshService).cancelTxn(eq(txn.getIgpCode()), txnCaptor.capture(), any());

		assertThat(txnCaptor.getValue().txnId).isEqualTo(txn.getTxnId());
		assertThat(txnCaptor.getValue().status).isEqualTo(TxnStatus.CANCELLED);
	}

	@Test
	@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL,
			PersistenceITBase.STAKE_TXN_SQL})
	public void cancelOkDemo()
	{
		AbstractTxn txn = txnService.cancel(TxnBuilder.txn().withMode(Mode.demo).build());

		assertThat(txn.getCancelTs()).isNotNull();
		assertThat(txn.getStatus()).isEqualTo(TxnStatus.CANCELLED);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'CANCELLED'"))
				.isEqualTo(1);

		ArgumentCaptor<Txn> txnCaptor = ArgumentCaptor.forClass(Txn.class);
		Mockito.verify(demoWalletService).cancelTxn(txnCaptor.capture(), any());

		assertThat(txnCaptor.getValue().txnId).isEqualTo(txn.getTxnId());
		assertThat(txnCaptor.getValue().status).isEqualTo(TxnStatus.CANCELLED);

	}

	@Test
	@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL, PersistenceITBase.STAKE_TXN_SQL})
	public void cancelOkBonus() {
		TxnRequest bonusRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();
		ArrayList<TxnEvent> requestList= new ArrayList<>();
		requestList.add(bonusRequest);

		AbstractTxn txn = txnService.cancel(TxnBuilder.txn().withMode(Mode.real).withTxnEvents(requestList).build());

		assertThat(txn.getCancelTs()).isNotNull();
		assertThat(txn.getStatus()).isEqualTo(TxnStatus.CANCELLED);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'CANCELLED'")).isEqualTo(1);

		ArgumentCaptor<Txn> txnCaptor = ArgumentCaptor.forClass(Txn.class);
		Mockito.verify(bonusWalletService).cancelTxn(eq(txn.getIgpCode()), txnCaptor.capture(), any());

		assertThat(txnCaptor.getValue().txnId).isEqualTo(txn.getTxnId());
		assertThat(txnCaptor.getValue().status).isEqualTo(TxnStatus.CANCELLED);
	}

	@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL, PersistenceITBase.STAKE_TXN_SQL})
	@Test
	public void cancelOkBonusWithMesh() {
		TxnRequest bonusRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();
		TxnRequest meshRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build())
				.build();

		ArrayList<TxnEvent> requestList= new ArrayList<>();
		requestList.add(bonusRequest);
		requestList.add(meshRequest);

		AbstractTxn txn = txnService.cancel(TxnBuilder.txn().withMode(Mode.real).withTxnEvents(requestList).build());

		assertThat(txn.getCancelTs()).isNotNull();
		assertThat(txn.getStatus()).isEqualTo(TxnStatus.CANCELLED);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'CANCELLED'")).isEqualTo(1);

		ArgumentCaptor<Txn> captor = ArgumentCaptor.forClass(Txn.class);

		Mockito.verify(bonusWalletService).cancelTxn(eq(txn.getIgpCode()), captor.capture(), any());

		assertThat(captor.getValue().getTxnId()).isEqualTo(TxnPresets.TXNID);
	}

	@Test
	@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL, PersistenceITBase.OPFRSTK_TXN_SQL})
	public void cancelOkOperatorFreeroundsStake() {
		TxnRequest operatorFreeroundsStakeRequest = defaultStakeTxnRequestBuilder()
				.txnType(TxnType.OPFRSTK)
				.bonusFundDetails(defaultOperatorBonusFundDetails().build())
				.build();
		ArrayList<TxnEvent> requestList = new ArrayList<>();
		requestList.add(operatorFreeroundsStakeRequest);

		Txn txn = TxnBuilder.txn()
				.withMode(Mode.real)
				.withType(TxnType.OPFRSTK)
				.withTxnEvents(requestList).build();
		cancelOk(txn);

		ArgumentCaptor<Txn> txnCaptor = ArgumentCaptor.forClass(Txn.class);
		Mockito.verify(meshService).cancelOperatorFreeroundsTxn(eq(txn.getIgpCode()), txnCaptor.capture(), any(), any());

		assertThat(txnCaptor.getValue().txnId).isEqualTo(txn.getTxnId());
		assertThat(txnCaptor.getValue().status).isEqualTo(TxnStatus.CANCELLED);
	}

	private void cancelOk(Txn txn) {
		Txn canceledTxn = txnService.cancel(txn);

		assertThat(canceledTxn.getCancelTs()).isNotNull();
		assertThat(canceledTxn.getStatus()).isEqualTo(TxnStatus.CANCELLED);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = '" + txn.getType() + "' and status = 'CANCELLED'")).isEqualTo(1);
	}

	@Test
	@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL, PersistenceITBase.STAKE_TXN_SQL})
	public void cancelFailureKnownException()
	{
		Mockito.doThrow(new ApiKnownException("ApiKnown", "")).when(meshService).cancelTxn(any(), any(), any());

		Txn txn = TxnBuilder.txn().withMode(Mode.real).build();
		boolean errorThrown = false;
		try {
			txnService.cancel(txn);
		} catch (ApiException e) {
			errorThrown = true;
			assertThat(e.getCode()).isEqualTo("ApiKnown");
		}
		assertThat(errorThrown).isTrue();

		assertThat(txn.getCancelTs()).isNull();
		assertThat(txn.getStatus()).isEqualTo(TxnStatus.RECON);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'RECON'")).isEqualTo(1);

		ArgumentCaptor<Txn> captor = ArgumentCaptor.forClass(Txn.class);
		Mockito.verify(meshService).cancelTxn(eq(txn.getIgpCode()), captor.capture(), any());

		assertThat(captor.getValue().getTxnId()).isEqualTo(txn.getTxnId());
		assertThat(captor.getValue().getStatus()).isEqualTo(TxnStatus.RECON);
	}

	@Test
	@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL, PersistenceITBase.STAKE_TXN_SQL})
	public void cancelFailureUnknownException()
	{
		Mockito.doThrow(new ApiUnknownException("ApiUnknown", "")).when(meshService).cancelTxn(any(), any(), any());

		Txn txn = TxnBuilder.txn().withMode(Mode.real).build();
		boolean errorThrown = false;

		try {
			txnService.cancel(txn);
		} catch (ApiException e) {
			errorThrown = true;
			assertThat(e.getCode()).isEqualTo("ApiUnknown");
		}
		assertThat(errorThrown).isEqualTo(true);

		assertThat(txn.getCancelTs()).isNull();
		assertThat(txn.getStatus()).isEqualTo(TxnStatus.PENDING);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'PENDING'")).isEqualTo(1);

		ArgumentCaptor<Txn> txnCaptor = ArgumentCaptor.forClass(Txn.class);
		Mockito.verify(meshService).cancelTxn(eq(txn.getIgpCode()), txnCaptor.capture(), any());

		assertThat(txnCaptor.getValue().txnId).isEqualTo(txn.getTxnId());
		assertThat(txnCaptor.getValue().status).isEqualTo(TxnStatus.PENDING);
	}

	@Test
	public void externalCancelOk()
	{
		//First process a txn
		when(meshService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
		String txnId = txnService.process(defaultStakeTxnRequestBuilder().build()).getTxnId();
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK'")).isEqualTo(1);

		//Now cancel it
		TxnReceipt receipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'CANCELLED'")).isEqualTo(1);

		Mockito.verify(meshService).cancelTxn(any(), any(), any());

		assertThat(receipt.getTxnId()).isEqualTo(txnId);
		assertThat(receipt.getStatus()).isEqualTo(TxnStatus.CANCELLED);
	}

	@Test
	public void givenStakePendingTransaction_whenExternalCancel_thenTransactionCancelled() {
		Mockito.doThrow(new ApiUnknownException("ApiUnknown", "")).when(meshService).sendTxn(any(), any());
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build()).getTxnId();
		} catch (ApiException e) {
			assertThat(e.getCode()).isEqualTo("ApiUnknown");
		}
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'PENDING'")).isEqualTo(1);

		//Now cancel it
		TxnReceipt receipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'CANCELLED'")).isEqualTo(1);

		Mockito.verify(meshService).cancelTxn(any(), any(), any());

		assertThat(receipt.getTxnId()).isEqualTo(TxnPresets.TXNID);
		assertThat(receipt.getStatus()).isEqualTo(TxnStatus.CANCELLED);
	}

	@Test
	public void givenStakeCancelledTransaction_whenExternalCancel_thenTransactionReceiptReturned() {
		when(meshService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		txnService.process(defaultStakeTxnRequestBuilder().build()).getTxnId();
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK'")).isEqualTo(1);

		TxnReceipt firstCancelReceipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'CANCELLED'")).isEqualTo(1);

		Mockito.verify(meshService).cancelTxn(any(), any(), any());

		TxnReceipt secondCancelReceipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertThat(secondCancelReceipt.getTxnId()).isEqualTo(firstCancelReceipt.getTxnId());
		assertThat(secondCancelReceipt.getStatus()).isEqualTo(firstCancelReceipt.getStatus());
		assertThat(secondCancelReceipt.getStatus()).isEqualTo(TxnStatus.CANCELLED);
	}

	@Test
	public void givenStakeFailedTransaction_whenExternalCancel_thenTransactionReceiptReturned() {
		when(meshService.sendTxn(any(),any())).thenThrow(new ApiKnownException("API_INSUFFICIENT_FUNDS","message"));
		try {
			txnService.process(defaultStakeTxnRequestBuilder().build()).getTxnId();
		} catch (ApiException e) {
			assertThat(e.getCode()).isEqualTo("API_INSUFFICIENT_FUNDS");
		}
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'FAILED'")).isEqualTo(1);

		TxnReceipt receipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'FAILED'")).isEqualTo(1);

		assertThat(receipt.getTxnId()).isEqualTo(TxnPresets.TXNID);
		assertThat(receipt.getStatus()).isEqualTo(TxnStatus.CANCELLED);
	}

	@Test
	@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL, PersistenceITBase.STAKE_TXN_SQL})
	public void givenStakeReconTransaction_whenExternalCancel_thenReconExceptionThrown() {
		Mockito.doThrow(new ApiKnownException("ApiKnown", "")).when(meshService).cancelTxn(any(), any(), any());
		Txn txn = TxnBuilder.txn().withMode(Mode.real).build();

		try {
			txnService.cancel(txn);
		} catch (ApiException e) {
			assertThat(e.getCode()).isEqualTo("ApiKnown");
		}

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'RECON'")).isEqualTo(1);

		try {
			txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());
		} catch (InternalServerException exception) {
			assertEquals("InternalServer", exception.getCode());
			assertEquals("Txn Previously Failed", exception.getMessage());
		}
	}

	@Test
	public void stakeOkWith256CharAuthToken() {

		Session session = new Session();
		session.setAccessToken(generateRandomString(256));
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);


		when(meshService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
		txnService.process(defaultStakeTxnRequestBuilder().build());
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK' and access_token = '" + session.getAccessToken() + "'")).isEqualTo(1);
	}

	@Test
	public void demoStakeOkWith256CharAuthToken() {
		Session session = new Session();
		session.setAccessToken(generateRandomString(256));
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);

		when(demoWalletService.sendTxn(any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());

		TxnRequest demoStake = defaultStakeTxnRequestBuilder().build();
		demoStake.setMode(Mode.demo);
		txnService.process(demoStake);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK' and access_token = '" + session.getAccessToken() + "'")).isEqualTo(1);
	}

	@Test
	public void bonusStakeOkWith256CharAuthToken() {
		Session session = new Session();
		session.setAccessToken(generateRandomString(256));
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);

		Wallet bonusWallet = WalletBuilder.freeroundsWallet().build();
		when(bonusWalletService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().withWallet(bonusWallet).build());

		TxnRequest stakeRequest = defaultStakeTxnRequestBuilder().build();
		stakeRequest.setBonusFundDetails(BonusFundDetailsPresets.defaultHiveBonusFundDetails().build());

		TxnReceipt receipt = txnService.process(stakeRequest);

		assertThat(receipt.getWallet().getFunds()).contains(FreeroundsFundBuilder.freeroundsFund().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK' and access_token = '" + session.getAccessToken() + "'")).isEqualTo(1);
	}

	@Test
	public void cancelStakeOkWith256CharAuthToken() {
		Session session = new Session();
		session.setAccessToken(generateRandomString(256));
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);

		//First process a txn
		when(meshService.sendTxn(any(),any())).thenReturn(TxnReceiptBuilder.txnReceipt().build());
		String txnId = txnService.process(defaultStakeTxnRequestBuilder().build()).getTxnId();
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'OK' and access_token = '" + session.getAccessToken() + "'")).isEqualTo(1);

		//Now cancel it
		TxnReceipt receipt = txnService.externalCancel(TxnCancelRequestBuilder.txnCancelRequest().build());

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'CANCELLED' and access_token = '" + session.getAccessToken() + "'")).isEqualTo(1);

		Mockito.verify(meshService).cancelTxn(any(), any(), any());

		assertThat(receipt.getTxnId()).isEqualTo(txnId);
		assertThat(receipt.getStatus()).isEqualTo(TxnStatus.CANCELLED);
	}
}
