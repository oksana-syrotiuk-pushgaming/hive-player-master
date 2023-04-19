package io.gsi.hive.platform.player.mesh;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.util.HttpClientUtils;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.event.EventType;
import io.gsi.hive.platform.player.mesh.gateway.MeshGateway;
import io.gsi.hive.platform.player.mesh.mapping.MeshHiveMapping;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuth;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerWrapper;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxn;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnCancel;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.session.PlayerLogin;
import io.gsi.hive.platform.player.session.SessionCreationLogin;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.GameplayWallet;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MeshService {

	private final MeshGateway gateway;
	private final MeshHiveMapping meshHiveMapping;

	public MeshService(MeshGateway gateway, MeshHiveMapping meshHiveMapping) {
		this.gateway = gateway;
		this.meshHiveMapping = meshHiveMapping;
	}

	public PlayerWrapper sendAuth(String igpCode, SessionCreationLogin login) {
		if(login.getType() != EventType.playerLogin) {
			throw new UnsupportedOperationException("Guest login not supported on mesh");
		}
		PlayerLogin playerLogin = (PlayerLogin) login;

		MeshPlayerAuth meshAuth = new MeshPlayerAuth(playerLogin.getAuthToken());
		MeshPlayerWrapper meshWrapper = gateway.authenticate(playerLogin.getPlayerId(), meshAuth, login.getGameCode(), createPlayerClient(playerLogin), igpCode);

		PlayerWrapper playerWrapper = meshHiveMapping.meshToHivePlayerWrapper(meshWrapper, igpCode);

		if (playerWrapper.getPlayer().getLang() == null){
			playerWrapper.getPlayer().setLang(login.getLang());
		}

		return playerWrapper;
	}

	public void validateGuestLaunch(String igpCode, String authToken) {
		gateway.validateGuestLaunch(igpCode, authToken);
	}

	public Player getPlayer(String igpCode, String playerId) {
		return meshHiveMapping.meshToHivePlayer(gateway.getPlayer(playerId, igpCode), igpCode);
	}

	public Wallet getWallet(String igpCode, String playerId, String gameCode, String accessToken) {
		MeshPlayerAuth meshAuth = new MeshPlayerAuth(accessToken);
		
		return meshHiveMapping.meshToHiveWallet(gateway.getWallet(playerId, gameCode, igpCode, meshAuth));
	}

	public GameplayWallet getGameplayWallet(String igpCode, String playerId, String gameCode, String accessToken) {
		MeshPlayerAuth meshAuth = new MeshPlayerAuth(accessToken);

		return meshHiveMapping.meshToGameplayWallet(gateway.getWallet(playerId, gameCode, igpCode, meshAuth));
	}

	public TxnReceipt sendTxn(String igpCode, Txn txn) {
		MeshGameTxn meshTxn = meshHiveMapping.hiveToMeshTxn(txn);
		return processMeshTxn(txn, meshTxn, igpCode);
	}

	public TxnReceipt sendOperatorFreeroundsTxn(String igpCode,
			Txn txn,
			OperatorBonusFundDetails bonusFundDetails) {
		MeshGameTxn meshTxn = meshHiveMapping.hiveToMeshOperatorFreeroundsTxn(txn, bonusFundDetails);
		return processMeshTxn(txn, meshTxn, igpCode);
	}

	public TxnReceipt sendFreeroundsWinTxn(String igpCode, Txn txn, Boolean playComplete,
			FreeroundsFund fund) {
		MeshGameTxn meshTxn = meshHiveMapping.hiveToMeshFreeroundsWinTxn(txn, playComplete, fund);
		return processMeshTxn(txn, meshTxn, igpCode);
	}

	public TxnReceipt sendFreeroundsCleardownTxn(BigDecimal cleardownWin, String igpCode, Txn txn, FreeroundsFund freeroundsFund) {
		MeshGameTxn meshTxn = meshHiveMapping.hiveToMeshFreeroundsCleardownTxn(cleardownWin, txn, freeroundsFund);
		return processMeshTxn(txn, meshTxn, igpCode);
	}
	
	private TxnReceipt processMeshTxn(Txn txn, MeshGameTxn meshTxn, String igpCode) {
		MeshPlayerAuth meshAuth = new MeshPlayerAuth(txn.getAccessToken());
		MeshGameTxnStatus gameTxnStatus = gateway.processTxn(meshAuth, meshTxn, igpCode);

		TxnReceipt txnReceipt = new TxnReceipt();
		txnReceipt.setTxnId(txn.getTxnId());
		txnReceipt.setGameCode(txn.getGameCode());
		txnReceipt.setTxnRef(gameTxnStatus.getIgpTxnId());
		txnReceipt.setPlayRef(gameTxnStatus.getIgpPlayId());
		txnReceipt.setWallet(meshHiveMapping.meshToHiveWallet(gameTxnStatus.getWallet()));
		txnReceipt.setStatus(TxnStatus.OK); //Mesh communicates failures via exception, so this is a fair assumption
		return txnReceipt;
	}

	public void cancelTxn(String igpCode, Txn txn, TxnCancel txnCancel) {
		MeshGameTxnCancel meshCancel = meshHiveMapping.hiveToMeshTxnCancel(txn, txnCancel);
		cancelTxn(igpCode, txn, txnCancel, meshCancel);
	}

	public void cancelBonusTxn(Txn txn, TxnCancel txnCancel, String igpCode) {

		if(txn.getAccessToken()== null) {
			throw new AuthorizationException("No authToken available to cancel txn : " +txn.getTxnId());
		}
		MeshPlayerAuth meshAuth = new MeshPlayerAuth(txn.getAccessToken());
		MeshGameTxnCancel meshCancel = meshHiveMapping.hiveToMeshTxnCancel(txn, txnCancel);
		String meshTxnId= txn.getEvents().stream().filter(TxnRequest.class::isInstance).map(TxnRequest.class::cast).findFirst().get().getTxnId();

		txn.addEvent(meshHiveMapping.createHiveTxnCancelRequest(meshTxnId, txn, txnCancel));
		
		gateway.cancelTxn(meshTxnId, meshCancel, igpCode, meshAuth);
	}

	public void cancelOperatorFreeroundsTxn(String igpCode, Txn txn, TxnCancel txnCancel,
											OperatorBonusFundDetails bonusFundDetails) {
		MeshGameTxnCancel meshCancel = meshHiveMapping.hiveToMeshOperatorFreeroundsTxnCancel(txn, txnCancel, bonusFundDetails);
		cancelTxn(igpCode, txn, txnCancel, meshCancel);
	}

	private void cancelTxn(String igpCode, Txn txn, TxnCancel txnCancel, MeshGameTxnCancel meshCancel) {
		if(txn.getAccessToken()== null) {
			throw new AuthorizationException("No authToken available to cancel txn : " +txn.getTxnId());
		}
		MeshPlayerAuth meshAuth = new MeshPlayerAuth(txn.getAccessToken());
		txn.addEvent(meshHiveMapping.createHiveTxnCancelRequest(txn.getTxnId(), txn, txnCancel));
		gateway.cancelTxn(txn.getTxnId(), meshCancel, igpCode, meshAuth);
	}

	private MeshPlayerClient createPlayerClient(PlayerLogin login)
	{
		MeshPlayerClient client = new MeshPlayerClient();

		//Convert hive to mesh
		client.setClientType(meshHiveMapping.hiveToMeshClientType(login.getClientType()));
		client.setIpAddress(login.getIpAddress());
		client.setUserAgent(login.getUserAgent());
		client.setChannel(getChannelFromUserAgent(login.getUserAgent()));

		return client;
	}

	private static MeshPlayerClient.Channel getChannelFromUserAgent(String userAgent) {
		if (userAgent == null) {
			return MeshPlayerClient.Channel.UNKNOWN;
		} else if (HttpClientUtils.isMobileUserAgent(userAgent)) {
			return MeshPlayerClient.Channel.MOBILE;
		} else if (HttpClientUtils.isTabletUserAgent(userAgent)) {
			return  MeshPlayerClient.Channel.TABLET;
		} else {
			return MeshPlayerClient.Channel.PC;
		}
	}
}
