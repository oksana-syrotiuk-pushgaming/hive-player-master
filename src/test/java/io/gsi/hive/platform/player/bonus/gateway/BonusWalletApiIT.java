package io.gsi.hive.platform.player.bonus.gateway;

import io.gsi.hive.platform.player.HivePlayer;
import io.gsi.hive.platform.player.bonus.BonusWalletService;
import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelBuilder;
import io.gsi.hive.platform.player.builders.TxnReceiptBuilder;
import io.gsi.hive.platform.player.builders.WalletBuilder;
import io.gsi.hive.platform.player.player.PlayerRepository;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.event.HiveBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.gsi.hive.platform.player.txn.TxnRequestPresets.defaultStakeTxnRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
@SpringBootTest(classes={HivePlayer.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class BonusWalletApiIT {

	@Autowired private BonusWalletService apiService;

	@MockBean private BonusWalletEndpoint endpoint;
	@MockBean private PlayerRepository playerRepository;

	private final String BONUS_WALLET_SERVICE_NAME = "hive-bonus-wallet-service-v1";

	@Test
	public void getWallet()
	{
		Wallet wallet = WalletBuilder.aWallet().withFunds(Arrays.asList(FreeroundsFundBuilder.freeroundsFund().build())).build();

		Mockito.when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
		 .thenReturn(Optional.of(wallet));

		Wallet receivedWallet = apiService.getWallet(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, GamePresets.CODE, PlayerPresets.CCY_CODE);

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());

		expectedHeader.get().add("BonusWallet-API-Key", "apiKey");
		Mockito.verify(endpoint).send(
				Mockito.eq("http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/wallet?playerId={playerId}&igpCode={igpCode}&gameCode={gameCode}&ccyCode={ccyCode}"),
				Mockito.eq(HttpMethod.GET),
				Mockito.eq(Optional.empty()),
				Mockito.eq(Optional.of(Wallet.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(BONUS_WALLET_SERVICE_NAME),
				Mockito.eq("player1"),
				Mockito.eq("iguana"),
				Mockito.eq("testGame"),
				Mockito.eq("GBP"));

		assertThat(wallet.getBalance()).isEqualTo(receivedWallet.getBalance());
		Fund expectedFund = FreeroundsFundBuilder.freeroundsFund().build();
		assertThat(wallet.getFunds().get(0)).isEqualTo(expectedFund);
	}

	@Test
	public void sendTxn()
	{
		TxnReceipt txnReceipt = TxnReceiptBuilder.txnReceipt().build();

		Mockito.when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString()))
		.thenReturn(Optional.of(txnReceipt));

		Txn txn = TxnBuilder.txn().build();
		TxnRequest txnRequest = defaultStakeTxnRequestBuilder()
				.bonusFundDetails(HiveBonusFundDetails.builder().build())
				.build();
		txn.setEvents(new ArrayList<>(List.of(txnRequest)));
		TxnReceipt receivedReceipt = apiService.sendTxn(IgpPresets.IGPCODE_IGUANA, txn);

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());

		expectedHeader.get().add("BonusWallet-API-Key", "apiKey");
		Mockito.verify(endpoint).send(
				Mockito.eq("http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/txn"),
				Mockito.eq(HttpMethod.POST),
				Mockito.any(),
				Mockito.eq(Optional.of(TxnReceipt.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(BONUS_WALLET_SERVICE_NAME));

		assertThat(txnReceipt).isEqualTo(receivedReceipt);
	}

	@Test
	public void cancelTxn()
	{
		TxnReceipt txnReceipt = TxnReceiptBuilder.txnReceipt().build();

		Mockito.when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString()))
		.thenReturn(Optional.of(txnReceipt));

		apiService.cancelTxn(IgpPresets.IGPCODE_IGUANA, TxnBuilder.txn().build(), TxnCancelBuilder.txnCancel().build());

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());

		expectedHeader.get().add("BonusWallet-API-Key", "apiKey");
		Mockito.verify(endpoint).send(
				Mockito.eq("http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/txn/{txnId}/cancel"),//Bonuswallet is internal, so concatenated id used
				Mockito.eq(HttpMethod.POST),
				Mockito.any(),
				Mockito.eq(Optional.of(TxnReceipt.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(BONUS_WALLET_SERVICE_NAME),
				Mockito.eq("1000-1"));
	}

	@Test
	public void closeFund(){
		apiService.closeFund(WalletPresets.BONUSFUNDID);

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());

		expectedHeader.get().add("BonusWallet-API-Key", "apiKey");
		Mockito.verify(endpoint).send(
				Mockito.eq("http://{bonusWalletServiceName}/hive/s2s/platform/bonuswallet/v1/wallet/fund/{fundId}/close"),
				Mockito.eq(HttpMethod.POST),
				Mockito.any(),
				Mockito.eq(Optional.empty()),
				Mockito.eq(expectedHeader),
				Mockito.eq(BONUS_WALLET_SERVICE_NAME),
				Mockito.eq(WalletPresets.BONUSFUNDID.toString()));
	}
}
