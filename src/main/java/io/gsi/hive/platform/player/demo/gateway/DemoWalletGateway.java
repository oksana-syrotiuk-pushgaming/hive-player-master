package io.gsi.hive.platform.player.demo.gateway;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.demo.player.GuestPlayer;
import io.gsi.hive.platform.player.demo.wallet.GuestWallet;
import io.gsi.hive.platform.player.demo.wallet.GuestWalletCreate;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;

@Loggable
@Service
public class DemoWalletGateway {

	private final DemoWalletEndpoint endpoint;
	private final String demoWalletApiKey;
	private final String demoWalletServiceName;

	public DemoWalletGateway(@Value("${hive.demoWalletServiceName:hive-demo-wallet-service-v1}")
									 String demoWalletServiceName,
							 @Value("${hive.demoWalletApiKey}")
									 String demoWalletApiKey,
							 DemoWalletEndpoint endpoint) {
		this.demoWalletServiceName = demoWalletServiceName;
		this.demoWalletApiKey = demoWalletApiKey;
		this.endpoint = endpoint;
	}

	public GuestPlayer createGuestPlayer(GuestLogin guestLogin) {
		return endpoint.send(
				"http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/player",
				HttpMethod.POST,
				Optional.of(guestLogin),
				Optional.of(GuestPlayer.class),
				Optional.of(getHeaders()),
				demoWalletServiceName
		).get();
	}


	public GuestWallet createWallet(GuestWalletCreate guestWalletCreate) {
		return endpoint.send(
				"http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/wallet",
				HttpMethod.POST,
				Optional.of(guestWalletCreate),
				Optional.of(GuestWallet.class),
				Optional.of(getHeaders()),
				demoWalletServiceName
		).get();
	}


	public GuestWallet getWallet(String playerId, String igpCode, String gameCode) {
		return endpoint.send(
				"http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/wallet?playerId={playerId}&igpCode={igpCode}&gameCode={gameCode}",
				HttpMethod.GET,
				Optional.empty(),
				Optional.of(GuestWallet.class),
				Optional.of(getHeaders()),
				demoWalletServiceName,
				playerId,
				igpCode,
				gameCode
		).get();
	}


	public TxnReceipt processTxn(TxnRequest txnRequest) {
		return endpoint.send(
				"http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/txn",
				HttpMethod.POST,
				Optional.of(txnRequest),
				Optional.of(TxnReceipt.class),
				Optional.of(getHeaders()),
				demoWalletServiceName
		).get();
	}

	public TxnReceipt cancelTxn(TxnCancelRequest txnCancel, String txnId) {
		return endpoint.send(
				"http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/txn/{txnId}/cancel",
				HttpMethod.POST,
				Optional.of(txnCancel),
				Optional.of(TxnReceipt.class),
				Optional.of(getHeaders()),
				demoWalletServiceName,
				txnId
		).get();
	}

	private HttpHeaders getHeaders()
	{
		HttpHeaders headers =  new HttpHeaders();
		headers.add("DemoWallet-API-Key", demoWalletApiKey);
		return headers;
	}

}

