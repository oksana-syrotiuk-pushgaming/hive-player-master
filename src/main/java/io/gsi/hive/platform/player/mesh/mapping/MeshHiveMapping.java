package io.gsi.hive.platform.player.mesh.mapping;

import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.bonus.wallet.OperatorFreeroundsFund;
import io.gsi.hive.platform.player.mesh.player.MeshPlayer;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerWrapper;
import io.gsi.hive.platform.player.mesh.txn.*;
import io.gsi.hive.platform.player.mesh.wallet.*;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.session.ClientType;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.AbstractTxn;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.event.*;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import io.gsi.hive.platform.player.wallet.GameplayWallet;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Static utils for mapping between Mesh and Hive Domains
 */
@Component
public class MeshHiveMapping {

	public static final String BONUS_FUND_DETAILS_KEY = "bonusFundDetails";

	private final Integer reconOffsetSeconds;
	private Boolean txnDeadlineOnWins;

	public MeshHiveMapping(@Value("${hive.recon.txn.offsetSeconds:30}") Integer reconOffsetSeconds,
						   @Value("${hive.recon.txn.deadlineOnWins:false}") boolean txnDeadlineOnWins) {
		this.reconOffsetSeconds = reconOffsetSeconds;
		setTxnDeadlineOnWins(txnDeadlineOnWins);
	}

	public void setTxnDeadlineOnWins(boolean txnDeadlineOnWins) {
		this.txnDeadlineOnWins = txnDeadlineOnWins;
	}

	public MeshGameTxn hiveToMeshTxn(AbstractTxn txn) {

		MeshGameTxnAction action = new MeshGameTxnAction(
				txn.getTxnId(),  //we don't have multiple actions per txn currently, so just use rgsTxnId
				hiveToMeshTxnAction(txn.getType()),
				txn.getAmount(),
				txn.getJackpotAmount());

		return new MeshGameTxn(
				txn.getTxnId(),
				txn.getGameCode(),
				txn.getPlayId(),
				txn.getRoundId(),
				txn.getPlayerId(),
				txn.getPlayComplete(),
				txn.getRoundComplete(),
				txn.getCcyCode(),
				Arrays.asList(action),
				buildTxnDeadline(txn),
				txn.getExtraInfo());
	}

	public TxnRequest createHiveFreeroundsCleardownTxnRequest(BigDecimal cleardownWin, String cleardownTxnId, AbstractTxn txn) {
		HiveBonusFundDetails bonusFundDetails = getBonusFundDetails(txn, HiveBonusFundDetails.class);

		return TxnRequest.builder()
				.amount(cleardownWin)
				.ccyCode(txn.getCcyCode())
				.bonusFundDetails(new HiveBonusFundDetails(bonusFundDetails.getFundId()))
				.gameCode(txn.getGameCode())
				.guest(false)
				.igpCode(txn.getIgpCode())
				.jackpotAmount(BigDecimal.ZERO)
				.mode(Mode.real)
				.playComplete(true)
				.playCompleteIfCancelled(txn.isPlayCompleteIfCancelled())
				.playerId(txn.getPlayerId())
				.playId(txn.getPlayId())
				.roundComplete(txn.getRoundComplete())
				.roundCompleteIfCancelled(txn.isRoundCompleteIfCancelled())
				.roundId(txn.getRoundId())
				.sessionId(txn.getSessionId())
				.txnId(cleardownTxnId)
				.txnType(TxnType.FRCLR)
				.build();
	}

	public TxnRequest createHiveFreeroundsWinTxnRequest(AbstractTxn txn) {
		HiveBonusFundDetails bonusFundDetails = getBonusFundDetails(
				txn, HiveBonusFundDetails.class);

		return TxnRequest.builder()
				.amount(txn.getAmount())
				.ccyCode(txn.getCcyCode())
				.bonusFundDetails(bonusFundDetails)
				.gameCode(txn.getGameCode())
				.guest(false)
				.igpCode(txn.getIgpCode())
				.jackpotAmount(BigDecimal.ZERO)
				.mode(Mode.real)
				.playComplete(txn.getPlayComplete())
				.playCompleteIfCancelled(txn.isPlayCompleteIfCancelled())
				.playerId(txn.getPlayerId())
				.playId(txn.getPlayId())
				.roundComplete(txn.getRoundComplete())
				.roundCompleteIfCancelled(txn.isRoundCompleteIfCancelled())
				.roundId(txn.getRoundId())
				.sessionId(txn.getSessionId())
				.txnId(txn.getTxnId())
				.txnType(TxnType.WIN)
				.build();
	}

	public TxnRequest createOperatorFreeroundsTxnRequest(AbstractTxn txn) {
		OperatorBonusFundDetails bonusFundDetails = getBonusFundDetails(
				txn, OperatorBonusFundDetails.class);

		return TxnRequest.builder()
				.amount(txn.getAmount())
				.ccyCode(txn.getCcyCode())
				.gameCode(txn.getGameCode())
				.guest(false)
				.igpCode(txn.getIgpCode())
				.jackpotAmount(txn.getJackpotAmount())
				.mode(Mode.valueOf(txn.getMode().name()))
				.playComplete(txn.getPlayComplete())
				.playCompleteIfCancelled(txn.isPlayCompleteIfCancelled())
				.playerId(txn.getPlayerId())
				.playId(txn.getPlayId())
				.roundComplete(txn.getRoundComplete())
				.roundCompleteIfCancelled(txn.isRoundCompleteIfCancelled())
				.roundId(txn.getRoundId())
				.sessionId(txn.getSessionId())
				.txnId(txn.getTxnId())
				.txnType(txn.getType())
				.extraInfo(txn.getExtraInfo())
				.bonusFundDetails(bonusFundDetails)
				.build();
	}

	private <T extends BonusFundDetails> T getBonusFundDetails(AbstractTxn txn, Class<T> bonusFundDetailsClass) {
		return txn.getEvents()
				.stream()
				.filter(TxnRequest.class::isInstance)
				.findFirst()
				.map(TxnRequest.class::cast)
				.map(TxnRequest::getBonusFundDetails)
				.map(bonusFundDetailsClass::cast)
				.orElseThrow();
	}

	public MeshGameTxn hiveToMeshFreeroundsWinTxn(Txn txn, Boolean playComplete, FreeroundsFund fund) {

		MeshGameTxnAction action = new MeshGameTxnAction(
				fund.getAwardId(),
				MeshGameTxnActionType.RGS_FREEROUND_WIN,
				txn.getAmount(),
				null);

		return new MeshGameTxn(
				txn.getTxnId(),
				txn.getGameCode(),
				txn.getPlayId(),
				txn.getRoundId(),
				txn.getPlayerId(),
				playComplete,
				txn.getRoundComplete(),
				txn.getCcyCode(),
				Arrays.asList(action),
				buildTxnDeadline(txn),
				txn.getExtraInfo());
	}

	public MeshGameTxn hiveToMeshFreeroundsCleardownTxn(BigDecimal cleardownWin, Txn txn, FreeroundsFund fund) {

		//Note that the request in the internal txn has the prepared id
		String bonusTxnId = ((TxnRequest) txn.getEvents().stream().filter(TxnRequest.class::isInstance).findFirst().get()).getTxnId();

		MeshGameTxnAction action = new MeshGameTxnAction(
				fund.getAwardId(),
				MeshGameTxnActionType.RGS_FREEROUND_CLEARDOWN,
				cleardownWin,
				null);

		return new MeshGameTxn(
				bonusTxnId,
				txn.getGameCode(),
				txn.getPlayId(),
				txn.getRoundId(),
				txn.getPlayerId(),
				//cleardown doesn't happen until the play is complete
				true,
				txn.getRoundComplete(),
				txn.getCcyCode(),
				Arrays.asList(action),
				buildTxnDeadline(txn),
				txn.getExtraInfo());
	}

	public MeshGameTxn hiveToMeshOperatorFreeroundsTxn(Txn txn, OperatorBonusFundDetails bonusFundDetails) {
		MeshOperatorFreeroundGameTxnAction action = new MeshOperatorFreeroundGameTxnAction(
				hiveToMeshTxnAction(txn.getType()),
				txn.getTxnId(),
				txn.getAmount(),
				txn.getJackpotAmount(),
				bonusFundDetails.getBonusId(),
				bonusFundDetails.getAwardId(),
				bonusFundDetails.getRemainingSpins(),
				bonusFundDetails.getExtraInfo());

		return new MeshGameTxn(
				txn.getTxnId(),
				txn.getGameCode(),
				txn.getPlayId(),
				txn.getRoundId(),
				txn.getPlayerId(),
				txn.getPlayComplete(),
				txn.getRoundComplete(),
				txn.getCcyCode(),
				List.of(action),
				buildTxnDeadline(txn),
				txn.getExtraInfo());
	}

	public MeshGameTxnActionType hiveToMeshTxnAction(TxnType txnType) {
		switch (txnType) {
			case STAKE:
				return MeshGameTxnActionType.STAKE;
			case WIN:
				return MeshGameTxnActionType.WIN;
			case OPFRSTK:
				return MeshGameTxnActionType.OPERATOR_FREEROUND_STAKE;
			case OPFRWIN:
				return MeshGameTxnActionType.OPERATOR_FREEROUND_WIN;
			default:
				throw new IllegalArgumentException("Unexpected TxnType " + txnType);
		}
	}

	public MeshGameTxnCancel hiveToMeshTxnCancel(AbstractTxn txn, TxnCancel txnCancel) {
		return MeshGameTxnCancel.builder()
				.playerId(txn.getPlayerId())
				.reason(null)
				.playComplete(txnCancel.isPlayComplete())
				.roundComplete(txnCancel.isRoundComplete())
				.rgsGameId(txn.getGameCode())
				.rgsRoundId(txn.getRoundId())
				.currency(txn.getCcyCode())
				.rgsPlayId(txn.getPlayId())
				.amount(txn.getAmount())
				.rgsTxnCancelId(null)
				.build();
	}

	public MeshGameTxnCancel hiveToMeshTxnCancel(Txn txn, TxnCancel txnCancel) {
		return MeshGameTxnCancel.builder()
				.playerId(txn.getPlayerId())
				.reason(null)
				.playComplete(txnCancel.isPlayComplete())
				.roundComplete(txnCancel.isRoundComplete())
				.rgsGameId(txn.getGameCode())
				.rgsRoundId(txn.getRoundId())
				.currency(txn.getCcyCode())
				.rgsPlayId(txn.getPlayId())
				.amount(txn.getAmount())
				.rgsTxnCancelId(null)
				.build();
	}

	public MeshGameTxnCancel hiveToMeshOperatorFreeroundsTxnCancel(Txn txn, TxnCancel txnCancel, OperatorBonusFundDetails operatorBonusFundDetails) {
		MeshGameTxnCancel meshGameTxnCancel = hiveToMeshTxnCancel(txn, txnCancel);
		meshGameTxnCancel.setExtraInfo(Map.of(BONUS_FUND_DETAILS_KEY, operatorBonusFundDetails));
		return meshGameTxnCancel;
	}

	//Not sent to mesh, but used for event audit trail
	public TxnCancelRequest createHiveTxnCancelRequest(String meshTxnId, AbstractTxn txn, TxnCancel txnCancel) {

		Optional<TxnCancelRequest> playerRequest = txn.getEvents().stream().filter(TxnCancelRequest.class::isInstance).map(TxnCancelRequest.class::cast).findFirst();

		TxnCancelRequest txnCancelRequest = new TxnCancelRequest();
		if (playerRequest.isPresent()) {
			txnCancelRequest.setCancelType(playerRequest.get().getCancelType());
		} else {
			txnCancelRequest.setCancelType(TxnCancelType.RECON);
		}
		txnCancelRequest.setGameCode(txn.getGameCode());
		txnCancelRequest.setPlayComplete(txnCancel.isPlayComplete());
		txnCancelRequest.setRoundComplete(txnCancel.isRoundComplete());
		txnCancelRequest.setTxnId(meshTxnId);

		return txnCancelRequest;
	}

	//Not sent to mesh, but used for event audit trail
	public TxnCancelRequest createHiveTxnCancelRequest(String meshTxnId, Txn txn, TxnCancel txnCancel) {

		Optional<TxnCancelRequest> playerRequest = txn.getEvents().stream().filter(TxnCancelRequest.class::isInstance).map(TxnCancelRequest.class::cast).findFirst();

		TxnCancelRequest txnCancelRequest = new TxnCancelRequest();
		if (playerRequest.isPresent()) {
			txnCancelRequest.setCancelType(playerRequest.get().getCancelType());
		} else {
			txnCancelRequest.setCancelType(TxnCancelType.RECON);
		}
		txnCancelRequest.setGameCode(txn.getGameCode());
		txnCancelRequest.setPlayComplete(txnCancel.isPlayComplete());
		txnCancelRequest.setRoundComplete(txnCancel.isRoundComplete());
		txnCancelRequest.setTxnId(meshTxnId);

		return txnCancelRequest;
	}

	/*Player*/
	public PlayerWrapper meshToHivePlayerWrapper(MeshPlayerWrapper meshPlayerWrapper, String igpCode) {
		PlayerWrapper playerWrapper = new PlayerWrapper();
		Player player = meshToHivePlayer(meshPlayerWrapper.getPlayer(), igpCode);
		Wallet wallet = meshToHiveWallet(meshPlayerWrapper.getPlayer().getWallet());
		playerWrapper.setPlayer(player);
		playerWrapper.setWallet(wallet);
		if (meshPlayerWrapper.getToken() != null) {  //mesh does not always have to return token
			playerWrapper.setAuthToken(meshPlayerWrapper.getToken().getToken());
		}
		return playerWrapper;
	}

	public Player meshToHivePlayer(MeshPlayer meshPlayer, String igpCode) {
		Player player = new Player();
		player.setAlias(meshPlayer.getAlias());
		player.setCcyCode(meshPlayer.getWallet().getCurrency());
		player.setGuest(false);  //guest play doesn't go through mesh
		player.setIgpCode(igpCode);
		player.setPlayerId(meshPlayer.getPlayerId());
		player.setUsername(meshPlayer.getUsername());
		player.setLang(meshPlayer.getLang());
		player.setCountry(meshPlayer.getCountry());
		return player;
	}

	/*Wallet*/
	public Fund meshToHiveFund(MeshWalletFund walletFund) {
		MeshWalletFundType walletFundType = walletFund.getType();
		switch (walletFundType) {
			case BONUS:
				return buildBalanceFund(FundType.BONUS, walletFund);
			case CASH:
				return buildBalanceFund(FundType.CASH, walletFund);
			case OPERATOR_FREEROUNDS:
				return buildOperatorFreeroundsFund(walletFund);
			default:
				return buildBalanceFund(FundType.UNKNOWN, walletFund);
		}
	}

	//Hive takes a Client type on login, Mesh uses one too but the domain objects are separate
	public io.gsi.hive.platform.player.mesh.player.MeshPlayerClient.ClientType hiveToMeshClientType(ClientType client) {
		return MeshPlayerClient.ClientType.valueOf(client.name());
	}

	public Wallet meshToHiveWallet(MeshWallet meshWallet) {
		Wallet wallet = new Wallet();
		wallet.setBalance(meshWallet.getBalance());
		wallet.setFunds(meshWallet.getFunds().stream()
				.map(this::meshToHiveFund)
				.collect(Collectors.toList()));
		wallet.setMessage(meshWallet.getMessage());
		return wallet;
	}

	public GameplayWallet meshToGameplayWallet(MeshWallet meshWallet) {
		GameplayWallet wallet = new GameplayWallet();
		wallet.setBalance(meshWallet.getBalance());
		wallet.setMessage(meshWallet.getMessage());
		wallet.setCcyCode(meshWallet.getCurrency());
		return wallet;
	}

	/**
	 * For stakes, txnDeadline is set to the time we will start attempting cancels.
	 * For wins we will not send one by default, but it can be turned on if symmetry is required.
	 */
	private ZonedDateTime buildTxnDeadline(AbstractTxn txn) {
		if (txn.isStake()) {
			return txn.getTxnTs().plus(reconOffsetSeconds, ChronoUnit.SECONDS);
		} else if (txnDeadlineOnWins) {
			return ZonedDateTime.now().plus(reconOffsetSeconds, ChronoUnit.SECONDS);
		} else {
			return null;
		}
	}

	private Fund buildBalanceFund(FundType fundType, MeshWalletFund meshWalletFund) {
		MeshWalletBalanceFund meshWalletBalanceFund = (MeshWalletBalanceFund) meshWalletFund;
		return new BalanceFund(fundType, meshWalletBalanceFund.getBalance());
	}

	private Fund buildOperatorFreeroundsFund(MeshWalletFund meshWalletFund) {
		MeshWalletOperatorFreeroundsFund operatorFreeroundsFund = (MeshWalletOperatorFreeroundsFund) meshWalletFund;
		return new OperatorFreeroundsFund(operatorFreeroundsFund.getBonusId(), operatorFreeroundsFund.getAwardId(), operatorFreeroundsFund.getExtraInfo());
	}
}
