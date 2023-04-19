package io.gsi.hive.platform.player.demo;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.commons.exception.InvalidStateException;
import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.demo.gateway.DemoWalletGateway;
import io.gsi.hive.platform.player.demo.guestauth.GuestAuthConfigService;
import io.gsi.hive.platform.player.demo.mapping.DemoWalletMapping;
import io.gsi.hive.platform.player.demo.player.GuestPlayer;
import io.gsi.hive.platform.player.demo.wallet.GuestWallet;
import io.gsi.hive.platform.player.demo.wallet.GuestWalletCreate;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerRepository;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.session.PlayerLogin;
import io.gsi.hive.platform.player.session.SessionCreationLogin;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.wallet.GameplayWallet;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DemoWalletService {
	private final MeshService meshService;
	private final DemoWalletGateway demoWalletGateway;
	private final PlayerRepository playerRepository;
	private final GuestAuthConfigService guestAuthConfigService;
	private final String demoCountry;
	private final String defaultDemoLang;

	public DemoWalletService(@Value("${hive.demoCountry:GB}") String demoCountry,
							 @Value("${hive.defaultDemoLang:en}") String demoLang,
							 MeshService meshService, PlayerRepository repository,
							 DemoWalletGateway gateway, GuestAuthConfigService guestAuthConfigService) {
		this.demoCountry = demoCountry;
		this.defaultDemoLang = demoLang;
		this.meshService = meshService;
		this.playerRepository = repository;
		this.demoWalletGateway = gateway;
		this.guestAuthConfigService = guestAuthConfigService;
	}

	public PlayerWrapper sendAuth(String igpCode, SessionCreationLogin login) {
		if (login instanceof PlayerLogin) {
			PlayerWrapper loggedInPlayerWrapper = meshService.sendAuth(igpCode,login);
			GuestWalletCreate walletCreate = DemoWalletMapping.playerLoginToGuestWalletCreate((PlayerLogin)login,loggedInPlayerWrapper);
			GuestWallet guestWallet = demoWalletGateway.createWallet(walletCreate);
			return DemoWalletMapping.playerAndGuestWalletToPlayerWrapper(loggedInPlayerWrapper.getPlayer(),guestWallet, loggedInPlayerWrapper.getAuthToken());
		} else if (login instanceof GuestLogin) {
			if(login.getCurrency() == null) {
				throw new BadRequestException("Currency must be set for Guest play");
			}

			GuestLogin guestLogin = (GuestLogin) login;

			if (guestAuthConfigService.isGuestValidationEnabledForIgp(igpCode)) {
				meshService.validateGuestLaunch(igpCode, guestLogin.getAuthToken());
			}

			GuestPlayer guestPlayer = demoWalletGateway.createGuestPlayer(guestLogin);

			Player player = DemoWalletMapping.guestPlayerAndLoginToPlayer(guestPlayer, guestLogin);
			player.setCountry(demoCountry);
			if(player.getLang() == null){
				player.setLang(defaultDemoLang);
			}
			playerRepository.save(player); // is this needed? the player gets saved later anyway

			GuestWalletCreate walletCreate = DemoWalletMapping.guestLoginAndPlayerToGuestWalletCreate(guestLogin, guestPlayer.getPlayerId());
			GuestWallet guestWallet = demoWalletGateway.createWallet(walletCreate);

			PlayerWrapper playerWrapper = DemoWalletMapping.playerAndGuestWalletToPlayerWrapper(player, guestWallet, guestLogin.getAuthToken());

			return playerWrapper;
		} else {
			throw new InvalidStateException("unknown login type");
		}
	}


	public Player getPlayer(String igpCode, String playerId) {
		Player player = playerRepository.findByPlayerIdAndIgpCode(playerId, igpCode);
		if (player == null) {
			throw new NotFoundException("Player Not Found");
		}
		return player;
	}


	public Wallet getWallet(String igpCode, String playerId, String gameCode) {
		return DemoWalletMapping.guestWalletToWallet(demoWalletGateway.getWallet(playerId, igpCode, gameCode));
	}

	public GameplayWallet getGameplayWallet(String igpCode, String playerId, String gameCode) {
		return DemoWalletMapping.guestWalletToGameplayWallet(demoWalletGateway.getWallet(playerId, igpCode, gameCode));
	}

	public TxnReceipt sendTxn(Txn txn) {
		TxnReceipt receipt = demoWalletGateway.processTxn(DemoWalletMapping.txnToTxnRequest(txn));
		receipt.setGameCode(txn.getGameCode());
		return receipt;
	}

	public void cancelTxn(Txn txn, TxnCancel txnCancel) {
		TxnCancelRequest txnCancelRequest = DemoWalletMapping.txnToTxnCancelRequest(txn, txnCancel);
		txn.addEvent(txnCancelRequest);

		demoWalletGateway.cancelTxn(txnCancelRequest, txn.getTxnId());
	}

}
