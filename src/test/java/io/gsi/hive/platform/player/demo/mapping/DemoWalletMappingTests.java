package io.gsi.hive.platform.player.demo.mapping;

import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.builders.*;
import io.gsi.hive.platform.player.demo.builders.GuestPlayerBuilder;
import io.gsi.hive.platform.player.demo.builders.GuestWalletBuilder;
import io.gsi.hive.platform.player.demo.player.GuestPlayer;
import io.gsi.hive.platform.player.demo.wallet.GuestWallet;
import io.gsi.hive.platform.player.demo.wallet.GuestWalletCreate;
import io.gsi.hive.platform.player.event.EventType;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnCancelType;
import io.gsi.hive.platform.player.txn.event.TxnEvent;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class DemoWalletMappingTests 
{
	@Test
	public void okGuestWalletToWallet() {
		GuestWallet guestWallet = GuestWalletBuilder.guestWallet().build();
		Wallet wallet = DemoWalletMapping.guestWalletToWallet(guestWallet);

		assertThat(wallet.getBalance()).isEqualTo(guestWallet.getBalance());

		Fund fund = new BalanceFund(FundType.CASH, guestWallet.getBalance());
		assertThat(wallet.getFunds().get(0)).isEqualTo(fund);
	}

	@Test
	public void okGuestToPlayer() {
		GuestPlayer guestPlayer = GuestPlayerBuilder.aPlayer().build();
		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().build();

		Player player = DemoWalletMapping.guestPlayerAndLoginToPlayer(guestPlayer, guestLogin);

		assertThat(player.getAlias()).isEqualTo(guestPlayer.getPlayerId());
		assertThat(player.getPlayerId()).isEqualTo(guestPlayer.getPlayerId());
		assertThat(player.getUsername()).isEqualTo(null);

		assertThat(player.getCcyCode()).isEqualTo(guestLogin.getCurrency());
		assertThat(player.getIgpCode()).isEqualTo(guestLogin.getIgpCode()).isEqualTo(guestPlayer.getIgpCode());
		assertThat(player.getLang()).isEqualTo(guestLogin.getLang());

		assertThat(player.getCountry()).isNull();
		assertThat(player.getGuest()).isTrue();
	}

	@Test
	public void okPlayerToWrapper() {
		Player player = PlayerBuilder.aPlayer().build();
		GuestWallet guestWallet = GuestWalletBuilder.guestWallet().build();

		PlayerWrapper playerWrapper = DemoWalletMapping.playerAndGuestWalletToPlayerWrapper(player, guestWallet, null);

		assertThat(playerWrapper.getAuthToken()).isNull();
		assertThat(playerWrapper.getPlayer()).isEqualTo(player);
		assertThat(playerWrapper.getWallet()).isEqualTo(DemoWalletMapping.guestWalletToWallet(guestWallet));
	}

	@Test
	public void okGuestLoginToWalletCreate() {
		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin().build();

		GuestWalletCreate guestWalletCreate = DemoWalletMapping.guestLoginAndPlayerToGuestWalletCreate(guestLogin, PlayerPresets.PLAYERID);

		assertThat(guestWalletCreate.getGuest()).isTrue();
		assertThat(guestWalletCreate.getCcyCode()).isEqualTo(guestLogin.getCurrency());
		assertThat(guestWalletCreate.getGameCode()).isEqualTo(guestLogin.getGameCode());
		assertThat(guestWalletCreate.getIgpCode()).isEqualTo(guestLogin.getIgpCode());
		assertThat(guestWalletCreate.getPlayerId()).isEqualTo(PlayerPresets.PLAYERID);
	}

	@Test
	public void TxnToRequest() {
		Txn demoTxn = TxnBuilder.txn().build();
		TxnRequest request = DemoWalletMapping.txnToTxnRequest(demoTxn);

		assertThat(request.getTxnId()).isEqualTo(demoTxn.getTxnId());
	}

	@Test
	public void txnCancelToCancelRequest() {
		TxnCancel cancel = TxnCancelBuilder.txnCancel().build();
		Txn demoTxn = TxnBuilder.txn().build();

		TxnCancelRequest cancelRequest = DemoWalletMapping.txnToTxnCancelRequest(demoTxn, cancel);

		assertThat(cancelRequest.getCancelType()).isEqualTo(TxnCancelType.RECON);

		assertThat(cancelRequest.getPlayComplete()).isEqualTo(cancel.isPlayComplete());
		assertThat(cancelRequest.getRoundComplete()).isEqualTo(cancel.isRoundComplete());

		assertThat(cancelRequest.getTxnId()).isEqualTo(demoTxn.getTxnId());
		assertThat(cancelRequest.getGameCode()).isEqualTo(demoTxn.getGameCode());

		assertThat(cancelRequest.getType()).isEqualTo(EventType.txnCancelRequest);
	}

	@Test
	public void txnWithCancelRequestCancelToCancelRequest() {
		TxnCancel cancel = TxnCancelBuilder.txnCancel().build();

		TxnCancelRequest cancelRequest = TxnCancelRequestBuilder.txnCancelRequest()
				.withCancelType(TxnCancelType.PLAYER)
				.build();

		ArrayList<TxnEvent> requests = new ArrayList<>();
		requests.add(cancelRequest);

		Txn demoTxn = TxnBuilder.txn().withTxnEvents(requests).build();

		TxnCancelRequest convertedCancelRequest = DemoWalletMapping.txnToTxnCancelRequest(demoTxn, cancel);

		assertThat(convertedCancelRequest.getCancelType()).isEqualTo(TxnCancelType.PLAYER);

		assertThat(convertedCancelRequest.getPlayComplete()).isEqualTo(cancel.isPlayComplete());
		assertThat(convertedCancelRequest.getRoundComplete()).isEqualTo(cancel.isRoundComplete());

		assertThat(convertedCancelRequest.getTxnId()).isEqualTo(demoTxn.getTxnId());
		assertThat(convertedCancelRequest.getGameCode()).isEqualTo(demoTxn.getGameCode());

		assertThat(cancelRequest.getType()).isEqualTo(EventType.txnCancelRequest);
	}
}
