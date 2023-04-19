package io.gsi.hive.platform.player.mesh.mapping;

import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.builders.PlayerBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.mesh.player.MeshGender;
import io.gsi.hive.platform.player.mesh.player.MeshPlayer;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerToken;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerWrapper;
import io.gsi.hive.platform.player.mesh.presets.MeshWalletPresets;
import io.gsi.hive.platform.player.mesh.txn.*;
import io.gsi.hive.platform.player.mesh.wallet.*;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.presets.AuthorizationPresets;
import io.gsi.hive.platform.player.presets.MonetaryPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.*;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;

import static io.gsi.hive.platform.player.txn.BonusFundDetailsPresets.defaultOperatorBonusFundDetails;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultWinTxnRequestBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MeshHiveMappingTests {

	private MeshHiveMapping meshHiveMapping;
	private int reconOffsetSeconds;

	@Before
	public void init() {
		reconOffsetSeconds = 30;
		meshHiveMapping = new MeshHiveMapping(reconOffsetSeconds, true);
	}

	@Test
	public void createOperatorFreeroundsTxnStakeRequest() {
		TxnRequest stakeRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(defaultOperatorBonusFundDetails().build())
				.build();
		AbstractTxn txn = exampleTxn();
		txn.setType(TxnType.OPFRSTK);
		txn.addEvent(stakeRequest);

		TxnRequest txnRequest = meshHiveMapping.createOperatorFreeroundsTxnRequest(txn);

		assertTxnRequest(txnRequest, txn, TxnType.OPFRSTK);
		assertThat(txnRequest.getBonusFundDetails(), equalTo(stakeRequest.getBonusFundDetails()));
	}

	@Test
	public void createOperatorFreeroundsTxnWinRequest() {
		TxnRequest winRequest = defaultWinTxnRequestBuilder()
				.bonusFundDetails(defaultOperatorBonusFundDetails().build())
				.build();
		AbstractTxn txn = exampleTxn();
		txn.setType(TxnType.OPFRWIN);
		txn.addEvent(winRequest);

		TxnRequest txnRequest = meshHiveMapping.createOperatorFreeroundsTxnRequest(txn);
		assertTxnRequest(txnRequest, txn, TxnType.OPFRWIN);
		assertThat(txnRequest.getBonusFundDetails(), equalTo(winRequest.getBonusFundDetails()));
	}

	@Test
	public void hiveToMeshTxnAction() {
		assertThat(meshHiveMapping.hiveToMeshTxnAction(TxnType.STAKE), equalTo(MeshGameTxnActionType.STAKE));
		assertThat(meshHiveMapping.hiveToMeshTxnAction(TxnType.WIN), equalTo(MeshGameTxnActionType.WIN));
	}

	@Test
	public void hiveToMeshTxnStake() {
		AbstractTxn txn = exampleTxn();
		ZonedDateTime expected = getExpectedTxnDeadline(txn.getTxnTs());

		MeshGameTxn gameTxn = meshHiveMapping.hiveToMeshTxn(txn);

		assertThat(gameTxn.getActions().size(), equalTo(1));
		assertThat(gameTxn.getActions().get(0).getType(), equalTo(MeshGameTxnActionType.STAKE));
		assertThat(gameTxn.getRgsTxnId(), equalTo("1000-1"));
		assertThat(gameTxn.getRgsPlayId(), equalTo("1000-1"));
		assertThat(gameTxn.getRgsGameId(), equalTo("testGame"));
		assertThat(gameTxn.getTxnDeadline(), greaterThanOrEqualTo(expected));
	}

	@Test
	public void hiveToMeshTxnWin() {
		AbstractTxn txn = exampleTxn();
		txn.setType(TxnType.WIN);
		meshHiveMapping.setTxnDeadlineOnWins(Boolean.FALSE);

		MeshGameTxn gameTxn = meshHiveMapping.hiveToMeshTxn(txn);

		assertThat(gameTxn.getActions().size(), equalTo(1));
		assertThat(gameTxn.getActions().get(0).getType(), equalTo(MeshGameTxnActionType.WIN));
		assertThat(gameTxn.getRgsTxnId(), equalTo("1000-1"));
		assertThat(gameTxn.getRgsPlayId(), equalTo("1000-1"));
		assertThat(gameTxn.getRgsGameId(), equalTo("testGame"));
		assertThat(gameTxn.getTxnDeadline(), nullValue());
	}

	@Test
	public void hiveToMeshTxnWinWithTxnDeadline() {
		AbstractTxn txn = exampleTxn();
		txn.setType(TxnType.WIN);

		meshHiveMapping.setTxnDeadlineOnWins(Boolean.TRUE);
		ZonedDateTime expectedTxnDeadline = getExpectedTxnDeadline(ZonedDateTime.now());

		MeshGameTxn gameTxn = meshHiveMapping.hiveToMeshTxn(txn);

		assertThat(gameTxn.getActions().size(), equalTo(1));
		assertThat(gameTxn.getActions().get(0).getType(), equalTo(MeshGameTxnActionType.WIN));
		assertThat(gameTxn.getRgsTxnId(), equalTo("1000-1"));
		assertThat(gameTxn.getRgsPlayId(), equalTo("1000-1"));
		assertThat(gameTxn.getRgsGameId(), equalTo("testGame"));
		assertThat(gameTxn.getTxnDeadline(), greaterThanOrEqualTo(expectedTxnDeadline));
	}

	@Test
	public void meshToHiveFund() {
		MeshWalletFund walletFund = new MeshWalletBalanceFund(MeshWalletFundType.CASH, BigDecimal.valueOf(100.00));

		BalanceFund fund = (BalanceFund) meshHiveMapping.meshToHiveFund(walletFund);

		assertThat(fund.getBalance(),  equalTo(BigDecimal.valueOf(100.00)));
		assertThat(fund.getType(),  equalTo(FundType.CASH));
	}

	@Test
	public void meshToHiveWallet() {
		MeshWallet meshWallet = meshWallet();

		Wallet wallet = meshHiveMapping.meshToHiveWallet(meshWallet);

		assertThat(wallet.getBalance(), equalTo(BigDecimal.valueOf(1000.00)));
		assertThat(wallet.getFunds().size(), equalTo(2));
		assertThat(((BalanceFund)wallet.getFunds().get(0)).getBalance(), equalTo(BigDecimal.valueOf(100.00)));
		assertThat(wallet.getFunds().get(0).getType(), equalTo(FundType.CASH));
		assertThat(((BalanceFund)wallet.getFunds().get(1)).getBalance(), equalTo(BigDecimal.valueOf(900.00)));
		assertThat(wallet.getFunds().get(1).getType(),  equalTo(FundType.BONUS));
		assertThat(wallet.getMessage(), equalTo(MeshWalletPresets.WALLET_MESSAGE));
	}

	@Test
	public void meshToHivePlayerWrapper() {
		MeshPlayer meshPlayer = new MeshPlayer(PlayerPresets.PLAYERID, PlayerPresets.USERNAME, PlayerPresets.ALIAS, "GB", "en", meshWallet(), MeshGender.Male, LocalDate.now(), null);
		MeshPlayerToken playerToken = new MeshPlayerToken("Bearer", AuthorizationPresets.ACCESSTOKEN, 300);
		MeshPlayerWrapper meshPlayerWrapper = new MeshPlayerWrapper(meshPlayer, playerToken);

		PlayerWrapper playerWrapper = meshHiveMapping.meshToHivePlayerWrapper(meshPlayerWrapper, "iguana");

		assertThat(playerWrapper.getWallet().getBalance(), equalTo(BigDecimal.valueOf(1000.00)));
		assertThat(playerWrapper.getPlayer(), equalTo(PlayerBuilder.aPlayer().build()));

		Fund fund1 = new BalanceFund(FundType.CASH, new BigDecimal("100.0"));
		Fund fund2 = new BalanceFund(FundType.BONUS, new BigDecimal("900.0"));

		Wallet expectedWallet = buildExpectedWallet("1000.0", fund1, fund2);
		assertThat(playerWrapper.getWallet(), equalTo(expectedWallet));
	}

	@Test
	public void meshToHiveTxnCancel() {
		AbstractTxn txn = exampleTxn();
		TxnCancel txnCancel = new TxnCancel();
		txnCancel.setPlayComplete(true);
		txnCancel.setRoundComplete(false);

		MeshGameTxnCancel gameTxnCancel = meshHiveMapping.hiveToMeshTxnCancel(txn, txnCancel);

		assertThat(gameTxnCancel.getPlayComplete(), equalTo(true));
		assertThat(gameTxnCancel.getRoundComplete(), equalTo(false));
		assertThat(gameTxnCancel.getPlayerId(), equalTo("player1"));
	}

	@Test
	public void hiveToMeshFreeroundsWinTxn() {
		Txn meshTxn = TxnBuilder.txn().build();

		MeshGameTxn meshGameTxn = meshHiveMapping.hiveToMeshFreeroundsWinTxn(meshTxn, Boolean.TRUE, FreeroundsFundBuilder
				.freeroundsFund().build());

		assertMeshGameTxn(meshTxn, meshGameTxn);

		MeshGameTxnAction meshTxnAction = (MeshGameTxnAction) meshGameTxn.getActions().get(0);
		assertThat(meshTxnAction.getType(), equalTo(MeshGameTxnActionType.RGS_FREEROUND_WIN));
		assertThat(meshTxnAction.getJackpotAmount(), equalTo(null));
		assertThat(meshTxnAction.getRgsActionId(), equalTo("award1"));
		assertThat(meshTxnAction.getAmount(), equalTo(MonetaryPresets.BDAMOUNT));
	}

	@Test
	public void hiveToMeshFreeroundsCleardownTxn() {
		Txn meshTxn = TxnBuilder.txn().build();

		MeshGameTxn meshGameTxn = meshHiveMapping.hiveToMeshFreeroundsCleardownTxn(MonetaryPresets.BDAMOUNT, meshTxn,
				FreeroundsFundBuilder.freeroundsFund().build());

		assertMeshGameTxn(meshTxn, meshGameTxn);

		MeshGameTxnAction meshTxnAction = (MeshGameTxnAction) meshGameTxn.getActions().get(0);
		assertThat(meshTxnAction.getType(), equalTo(MeshGameTxnActionType.RGS_FREEROUND_CLEARDOWN));
		assertThat(meshTxnAction.getJackpotAmount(), equalTo(null));
		assertThat(meshTxnAction.getRgsActionId(), equalTo("award1"));
		assertThat(meshTxnAction.getAmount(), equalTo(MonetaryPresets.BDAMOUNT));
	}

	@Test
	public void hiveToMeshOperatorFreeroundsStakeTxn() {
		hiveToMeshOperatorFreeroundsTxn(TxnType.OPFRSTK, MeshGameTxnActionType.OPERATOR_FREEROUND_STAKE);
	}

	@Test
	public void hiveToMeshOperatorFreeroundsWinTxn() {
		hiveToMeshOperatorFreeroundsTxn(TxnType.OPFRWIN, MeshGameTxnActionType.OPERATOR_FREEROUND_WIN);
	}

	@Test
	public void hiveToMeshOperatorFreeroundsTxnCancel() {
		Txn meshTxn = TxnBuilder.txn().build();

		TxnCancel txnCancel = new TxnCancel();
		txnCancel.setPlayComplete(true);
		txnCancel.setRoundComplete(false);

		OperatorBonusFundDetails operatorBonusFundDetails = defaultOperatorBonusFundDetails().build();

		MeshGameTxnCancel meshGameTxnCancel = meshHiveMapping.hiveToMeshOperatorFreeroundsTxnCancel(
				meshTxn, txnCancel, operatorBonusFundDetails);

		assertThat(meshGameTxnCancel.getRgsPlayId(), equalTo(meshTxn.getPlayId()));
		assertThat(meshGameTxnCancel.getRgsRoundId(), equalTo(meshTxn.getRoundId()));
		assertThat(meshGameTxnCancel.getRgsTxnCancelId(), is(nullValue()));
		assertThat(meshGameTxnCancel.getPlayerId(), equalTo(meshTxn.getPlayerId()));
		assertThat(meshGameTxnCancel.getAmount(), equalTo(meshTxn.getAmount()));
		assertThat(meshGameTxnCancel.getRgsGameId(), equalTo(meshTxn.getGameCode()));
		assertThat(meshGameTxnCancel.getPlayComplete(), equalTo(txnCancel.isPlayComplete()));
		assertThat(meshGameTxnCancel.getRoundComplete(), equalTo(txnCancel.isRoundComplete()));
		assertThat(meshGameTxnCancel.getReason(), is(nullValue()));
		assertThat(meshGameTxnCancel.getCurrency(), equalTo(meshTxn.getCcyCode()));
		assertThat(meshGameTxnCancel.getExtraInfo(), equalTo(Map.of("bonusFundDetails", operatorBonusFundDetails)));
	}

	private void hiveToMeshOperatorFreeroundsTxn(TxnType txnType, MeshGameTxnActionType gameTxnActionType) {
		Txn meshTxn = TxnBuilder.txn()
				.withType(txnType)
				.build();

		MeshGameTxn meshGameTxn = meshHiveMapping.hiveToMeshOperatorFreeroundsTxn(meshTxn,
				defaultOperatorBonusFundDetails().build());

		assertMeshGameTxn(meshTxn, meshGameTxn);

		MeshOperatorFreeroundGameTxnAction meshTxnAction = (MeshOperatorFreeroundGameTxnAction) meshGameTxn.getActions().get(0);
		assertThat(meshTxnAction.getType(), equalTo(gameTxnActionType));
		assertThat(meshTxnAction.getBonusId(), equalTo(WalletPresets.BONUS_ID));
		assertThat(meshTxnAction.getAwardId(), equalTo(WalletPresets.AWARD_ID));
		assertThat(meshTxnAction.getFreeroundsRemaining(), equalTo(WalletPresets.FREEROUNDS_REMAINING));
		assertThat(meshTxnAction.getExtraInfo(), equalTo(WalletPresets.EXTRA_INFO));
	}

	private void assertMeshGameTxn(Txn meshTxn, MeshGameTxn meshGameTxn) {
		assertThat(meshGameTxn.getRgsTxnId(), equalTo(meshTxn.getTxnId()));
		assertThat(meshGameTxn.getRgsPlayId(), equalTo(meshTxn.getPlayId()));
		assertThat(meshGameTxn.getRgsGameId(), equalTo(meshTxn.getGameCode()));
		assertThat(meshGameTxn.getCurrency(), equalTo(meshTxn.getCcyCode()));
		assertThat(meshGameTxn.getPlayComplete(), equalTo(meshTxn.getPlayComplete()));
		assertThat(meshGameTxn.getRoundComplete(), equalTo(meshTxn.getRoundComplete()));
		assertThat(meshGameTxn.getPlayerId(), equalTo(meshTxn.getPlayerId()));
		assertThat(meshGameTxn.getRgsRoundId(), equalTo(meshTxn.getRoundId()));
		assertThat(meshGameTxn.getExtraInfo(), equalTo(meshTxn.getExtraInfo()));
		assertThat(meshGameTxn.getTxnDeadline(), greaterThanOrEqualTo(getExpectedTxnDeadline(meshTxn.getTxnTs())));
	}

	private void assertTxnRequest(TxnRequest txnRequest, AbstractTxn txn, TxnType txnType) {
		assertThat(txnRequest.getAmount(), equalTo(txn.getAmount()));
		assertThat(txnRequest.getCcyCode(), equalTo(txn.getCcyCode()));
		assertThat(txnRequest.getGuest(), equalTo(txn.getGuest()));
		assertThat(txnRequest.getIgpCode(), equalTo(txn.getIgpCode()));
		assertThat(txnRequest.getJackpotAmount(), equalTo(txn.getJackpotAmount()));
		assertThat(txnRequest.getMode(), equalTo(txn.getMode()));
		assertThat(txnRequest.getPlayComplete(), equalTo(txn.getPlayComplete()));
		assertThat(txnRequest.getRoundCompleteIfCancelled(), equalTo(txn.isPlayCompleteIfCancelled()));
		assertThat(txnRequest.getPlayerId(), equalTo(txn.getPlayerId()));
		assertThat(txnRequest.getPlayId(), equalTo(txn.getPlayId()));
		assertThat(txnRequest.getRoundComplete(), equalTo(txn.getRoundComplete()));
		assertThat(txnRequest.getRoundCompleteIfCancelled(), equalTo(txn.isRoundCompleteIfCancelled()));
		assertThat(txnRequest.getRoundId(), equalTo(txn.getRoundId()));
		assertThat(txnRequest.getSessionId(), equalTo(txn.getSessionId()));
		assertThat(txnRequest.getTxnId(), equalTo(txn.getTxnId()));
		assertThat(txnRequest.getTxnType(), equalTo(txnType));
		assertThat(txnRequest.getExtraInfo(), equalTo(txn.getExtraInfo()));
	}

	MeshWallet meshWallet() {
		MeshWalletFund walletFundCash =
				new MeshWalletBalanceFund(MeshWalletFundType.CASH, BigDecimal.valueOf(100.00));
		MeshWalletFund walletFundBonus =
				new MeshWalletBalanceFund(MeshWalletFundType.BONUS, BigDecimal.valueOf(900.00));
		return new MeshWallet(MeshWalletType.ACCOUNT, "GBP", BigDecimal.valueOf(1000.00),
				Arrays.asList(walletFundCash, walletFundBonus), MeshWalletPresets.WALLET_MESSAGE);
	}

	private Wallet buildExpectedWallet(String balance, Fund... funds) {
		return WalletBuilder.aWallet()
				.withBalance(new BigDecimal(balance))
				.withFunds(Arrays.asList(funds))
				.withMessage(MeshWalletPresets.WALLET_MESSAGE)
				.build();
	}

	private AbstractTxn exampleTxn() {
		AbstractTxn txn = new Txn();
		txn.setAccessToken("accessToken");
		txn.setAmount(BigDecimal.valueOf(5.00));
		txn.setBalance(BigDecimal.valueOf(10.00));
		txn.setCcyCode("GBP");
		txn.setGameCode("testGame");
		txn.setIgpCode("iguana");
		txn.setJackpotAmount(BigDecimal.ZERO);
		txn.setPlayComplete(true);
		txn.setPlayerId("player1");
		txn.setPlayId("1000-1");
		txn.setRoundComplete(true);
		txn.setRoundId("1000-1");
		txn.setStatus(TxnStatus.OK);
		txn.setTxnId("1000-1");
		txn.setType(TxnType.STAKE);
		txn.setMode(Mode.real);
		txn.setGuest(false);
		return txn;
	}

	private ZonedDateTime getExpectedTxnDeadline(ZonedDateTime txnTs) {
		return txnTs.plus(reconOffsetSeconds, ChronoUnit.SECONDS);
	}
}
