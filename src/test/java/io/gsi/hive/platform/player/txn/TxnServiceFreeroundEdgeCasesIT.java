/**
 * © gsi.io 2016
 */
package io.gsi.hive.platform.player.txn;

import static io.gsi.hive.platform.player.txn.BonusFundDetailsPresets.defaultHiveBonusFundDetails;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;


import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.demo.DemoWalletService;
import io.gsi.hive.platform.player.exception.FreeroundsFundNotAvailableException;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.play.PlayService;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.session.Session;
import io.gsi.hive.platform.player.session.SessionService;
import io.gsi.hive.platform.player.txn.event.TxnRequest;

@Sql(statements={PersistenceITBase.CLEAN_DB_SQL, PersistenceITBase.PLAYER_SQL}, executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements={PersistenceITBase.CLEAN_DB_SQL}, executionPhase= Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class TxnServiceFreeroundEdgeCasesIT extends ApiITBase {
	
	private JdbcTemplate jdbcTemplate;
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	//Dont mock InternalTxnService
	@Autowired private TxnService txnService;	
	@MockBean private MeshService meshService;
	@MockBean private DemoWalletService demoWalletService;
	@MockBean private SessionService sessionService;
	@MockBean private BonusWalletService bonusWalletService;
	@MockBean private PlayService playService;

	@Before
	public void setup() {
		Session session = new Session();
		session.setAccessToken("testToken");
		when(sessionService.getSession(Mockito.anyString())).thenReturn(session);
	}

	@Test
	public void givenFreeroundFundNotAvailable_whenProcessStakeTxn_thenPersistFailedStakeAndCancelInPlay(){
		when(bonusWalletService.sendTxn(any(),any())).thenThrow(new FreeroundsFundNotAvailableException(""));

		TxnRequest stakeRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(defaultHiveBonusFundDetails().build())
				.build();

		assertThatThrownBy(() -> txnService.process(stakeRequest)).isInstanceOf(FreeroundsFundNotAvailableException.class);

		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'STAKE' and status = 'FAILED'")).isEqualTo(1);

		Mockito.verify(bonusWalletService).sendTxn(any(), any());
		Mockito.verify(meshService, never()).sendFreeroundsWinTxn(any(), any(), any(), any());
		Mockito.verify(playService).cancelStake(Mockito.any());
	}
	
	@Test
	public void givenFreeroundFundNotAvailable_whenProcessWinTxn_thenPersistFailedWinTxnAndVoidPlay(){
		when(bonusWalletService.sendTxn(any(),any())).thenThrow(new FreeroundsFundNotAvailableException(""));

		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(defaultHiveBonusFundDetails().build())
				.build();

		assertThatThrownBy(() -> txnService.process(winRequest)).isInstanceOf(FreeroundsFundNotAvailableException.class);
		assertThat(JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,"t_txn",
				"txn_id = '" + TxnPresets.TXNID + "' and type = 'WIN' and status = 'FAILED'")).isEqualTo(1);

		Mockito.verify(bonusWalletService).sendTxn(any(), any());
		Mockito.verify(meshService, never()).sendFreeroundsWinTxn(any(), any(), any(), any());
		Mockito.verify(playService).voidPlay(Mockito.anyString());
	}

	//Old behaviour - shouldn't do this anymore, hence InvalidState
	@Test
	public void givenBonusWalletReturnsNoFunds_whenProcessWinTxn_thenPersistReconWinTxn(){
		when(bonusWalletService.sendTxn(any(),any())).thenThrow(new InvalidStateException(""));

		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(defaultHiveBonusFundDetails().build())
				.build();

		assertThatThrownBy(() -> txnService.process(winRequest)).isInstanceOf(InvalidStateException.class);

		Mockito.verify(bonusWalletService).sendTxn(any(), any());
		Mockito.verify(meshService, never()).sendFreeroundsWinTxn(any(), any(), any(), any());
	}
}
