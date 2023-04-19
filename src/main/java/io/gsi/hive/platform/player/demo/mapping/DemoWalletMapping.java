package io.gsi.hive.platform.player.demo.mapping;

import io.gsi.hive.platform.player.demo.player.GuestPlayer;
import io.gsi.hive.platform.player.demo.wallet.GuestWallet;
import io.gsi.hive.platform.player.demo.wallet.GuestWalletCreate;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.session.PlayerLogin;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnCancelType;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.GameplayWallet;
import io.gsi.hive.platform.player.wallet.Wallet;

import java.util.Optional;

public class DemoWalletMapping {

	public static Wallet guestWalletToWallet(GuestWallet guestWallet)
	{
		Wallet wallet = new Wallet();

		wallet.setFunds(guestWallet.getFunds());
		wallet.setBalance(guestWallet.getBalance());

		return wallet;
	}

	public static GameplayWallet guestWalletToGameplayWallet(GuestWallet guestWallet) {
		GameplayWallet wallet = new GameplayWallet();

		wallet.setCcyCode(guestWallet.getCcyCode());
		wallet.setBalance(guestWallet.getBalance());

		return wallet;
	}

	public static Player guestPlayerAndLoginToPlayer(GuestPlayer guestPlayer, GuestLogin login)
	{
		Player player = new Player();

		player.setAlias(guestPlayer.getPlayerId());
		player.setIgpCode(guestPlayer.getIgpCode());
		player.setPlayerId(guestPlayer.getPlayerId());
		player.setUsername(guestPlayer.getPlayerId());

		player.setCcyCode(login.getCurrency());

		//Wired in api service
		player.setCountry(null);
		
		player.setGuest(true);
		player.setLang(login.getLang());

		return player;
	}

	public static PlayerWrapper playerAndGuestWalletToPlayerWrapper(Player player, GuestWallet guestWallet, String authToken)
	{
		PlayerWrapper wrapper = new PlayerWrapper();

		wrapper.setAuthToken(authToken);
		wrapper.setPlayer(player);
		wrapper.setWallet(guestWalletToWallet(guestWallet));

		return wrapper;
	}

	public static GuestWalletCreate guestLoginAndPlayerToGuestWalletCreate(GuestLogin guestLogin, String playerId)
	{
		GuestWalletCreate walletCreate = new GuestWalletCreate();

		walletCreate.setCcyCode(guestLogin.getCurrency());
		walletCreate.setGameCode(guestLogin.getGameCode());
		walletCreate.setGuest(true);
		walletCreate.setIgpCode(guestLogin.getIgpCode());
		walletCreate.setPlayerId(playerId);

		return walletCreate;
	}

	public static GuestWalletCreate playerLoginToGuestWalletCreate(PlayerLogin playerLogin,PlayerWrapper wrapper)
	{
		GuestWalletCreate walletCreate = new GuestWalletCreate();
		walletCreate.setCcyCode(Optional.ofNullable(playerLogin.getCurrency()).orElse(wrapper.getPlayer().getCcyCode()));
		walletCreate.setGameCode(playerLogin.getGameCode());
		walletCreate.setGuest(false);
		walletCreate.setIgpCode(playerLogin.getIgpCode());
		walletCreate.setPlayerId(playerLogin.getPlayerId());

		return walletCreate;
	}

	public static TxnRequest txnToTxnRequest(Txn txn)
	{
		return TxnRequest.builder()
				.amount(txn.getAmount())
				.ccyCode(txn.getCcyCode())
				.gameCode(txn.getGameCode())
				.guest(txn.getGuest())
				.igpCode(txn.getIgpCode())
				.jackpotAmount(txn.getJackpotAmount())
				.mode(txn.getMode())
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
				.build();
	}

	public static TxnCancelRequest txnToTxnCancelRequest(Txn txn, TxnCancel cancel)
	{
		TxnCancelRequest cancelRequest = new TxnCancelRequest();

		/**Due to a bug in this code, a txnCancelRequest will be added to the txn we are searching in each time a cancel is attempted,
		 * e.g. during recon when retries are necessary.
		 * If there is actually an external request however, it will be the correct type, as the first type extracted will be propagated 
		 * down through the duplicates.
		 * */
		Optional<TxnCancelRequest> playerRequest = txn.getEvents().stream().filter(TxnCancelRequest.class::isInstance).map(TxnCancelRequest.class::cast).findFirst();

		//If the cancel was started externally, use the type from request, 
		//if not it must have been recon
		if(playerRequest.isPresent()) {
			cancelRequest.setCancelType(playerRequest.get().getCancelType());
		}
		else {
			cancelRequest.setCancelType(TxnCancelType.RECON);
		}

		cancelRequest.setGameCode(txn.getGameCode());
		cancelRequest.setPlayComplete(cancel.isPlayComplete());
		cancelRequest.setRoundComplete(cancel.isRoundComplete());
		cancelRequest.setTxnId(txn.getTxnId());

		return cancelRequest;
	}
}
