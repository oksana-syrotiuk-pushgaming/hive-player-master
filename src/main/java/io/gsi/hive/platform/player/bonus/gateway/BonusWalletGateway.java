package io.gsi.hive.platform.player.bonus.gateway;

import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.txn.event.TxnCancelRequest;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.Wallet;

@Loggable
@Service
public class BonusWalletGateway {

	private final BonusWalletEndpoint endpoint;
	private String bonusWalletApiKey;
	private String bonusWalletServiceName;

	public BonusWalletGateway(@Value("${hive.bonusWalletServiceName:hive-bonus-wallet-service-v1}")
									  String bonusWalletServiceName,
							  @Value("${hive.bonusWalletApiKey}")
									  String bonusWalletApiKey,
							  BonusWalletEndpoint endpoint) {
		this.bonusWalletServiceName = bonusWalletServiceName;
		this.bonusWalletApiKey = bonusWalletApiKey;
		this.endpoint = endpoint;
	}

	public Wallet getWallet(String playerId, String igpCode, String gameCode, String ccyCode) {
		return endpoint.send(
				"http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/wallet?playerId={playerId}&igpCode={igpCode}&gameCode={gameCode}&ccyCode={ccyCode}",
				HttpMethod.GET,
				Optional.empty(),
				Optional.of(Wallet.class),
				Optional.of(getHeaders()),
				bonusWalletServiceName,
				playerId,
				igpCode,
				gameCode,
				ccyCode
		).get();
	}


	public TxnReceipt processTxn(TxnRequest txnRequest) {
		return endpoint.send(
				"http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/txn",
				HttpMethod.POST,
				Optional.of(txnRequest),
				Optional.of(TxnReceipt.class),
				Optional.of(getHeaders()),
				bonusWalletServiceName
		).get();
	}

	public TxnReceipt cancelTxn(TxnCancelRequest txnCancel, String txnId) {
		return endpoint.send(
				"http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/txn/{txnId}/cancel",
				HttpMethod.POST,
				Optional.of(txnCancel),
				Optional.of(TxnReceipt.class),
				Optional.of(getHeaders()),
				bonusWalletServiceName,
				txnId
		).get();
	}

	public void closeFund(Long fundId) {
		endpoint.send(
				"http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/wallet/fund/{fundId}/close",
				HttpMethod.POST,
				Optional.empty(),
				Optional.empty(),
				Optional.of(getHeaders()),
				bonusWalletServiceName,
				fundId.toString()
		);
	}

	private HttpHeaders getHeaders()
	{
		HttpHeaders headers =  new HttpHeaders();
		headers.add("BonusWallet-API-Key", bonusWalletApiKey);
		return headers;
	}

  public FreeRoundsBonusPlayerAwardStatus getBonusAwardStatus(String igpCode, Long fundId) {
		return endpoint.send(
				"http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/igp/{igpCode}/fund/{fundId}/status",
				HttpMethod.GET,
				Optional.empty(),
				Optional.of(FreeRoundsBonusPlayerAwardStatus.class),
				Optional.of(getHeaders()),
				bonusWalletServiceName,
				igpCode,
				fundId.toString()
		).get();
  }
}

