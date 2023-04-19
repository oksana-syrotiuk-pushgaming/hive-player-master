package io.gsi.hive.platform.player.demo.gateway;

import io.gsi.commons.exception.NotFoundException;
import io.gsi.hive.platform.player.HivePlayer;
import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.builders.*;
import io.gsi.hive.platform.player.demo.DemoWalletService;
import io.gsi.hive.platform.player.demo.builders.GuestPlayerBuilder;
import io.gsi.hive.platform.player.demo.player.GuestPlayer;
import io.gsi.hive.platform.player.demo.wallet.GuestWallet;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.player.Player;
import io.gsi.hive.platform.player.player.PlayerRepository;
import io.gsi.hive.platform.player.player.PlayerWrapper;
import io.gsi.hive.platform.player.presets.AuthorizationPresets;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.session.PlayerLogin;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.wallet.Fund;
import io.gsi.hive.platform.player.wallet.FundType;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Optional;

import static io.gsi.hive.platform.player.builders.PlayerBuilder.aPlayer;
import static io.gsi.hive.platform.player.builders.WalletBuilder.aWallet;
import static io.gsi.hive.platform.player.demo.builders.GuestWalletBuilder.guestWallet;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
@SpringBootTest(classes={HivePlayer.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties={"hive.demoWalletServiceName=hive-demo-wallet-service-v1"})
public class DemoWalletApiIT {

	@Autowired private DemoWalletGateway demoWalletGateway;
	@Autowired private DemoWalletService apiService;

	@MockBean private DemoWalletEndpoint endpoint;
	@MockBean private PlayerRepository playerRepository;
	@MockBean private MeshService meshService;

	@Value("${hive.demoWalletServiceName}")
	private String demoWalletServiceName;

	@Test
	public void okAuthGuestLogin()
	{
		Mockito.when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString()))
		.thenReturn(Optional.of(GuestPlayerBuilder.aPlayer().build()))
		.thenReturn(Optional.of(guestWallet().build()));

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin()
				.withMode(Mode.demo)
				.withAuthToken(AuthorizationPresets.ACCESSTOKEN)
				.build();

		PlayerWrapper playerWrapper = apiService.sendAuth(IgpPresets.IGPCODE_IGUANA, guestLogin);

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());
		expectedHeader.get().add("DemoWallet-API-Key", "apiKey");

		assertThat(playerWrapper.getAuthToken()).isEqualTo(guestLogin.getAuthToken());

		Mockito.verify(meshService, Mockito.times(1))
				.validateGuestLaunch(IgpPresets.IGPCODE_IGUANA, AuthorizationPresets.ACCESSTOKEN);

		Mockito.verify(endpoint).send(
				Mockito.eq("http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/player"),
				Mockito.eq(HttpMethod.POST),
				Mockito.eq(Optional.of(guestLogin)),
				Mockito.eq(Optional.of(GuestPlayer.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(demoWalletServiceName)
		);
	}

	@Test
	public void givenGuestLoginWithNoGuestValidationForIgpCode_whenSendAuth_returnOk()
	{
		Mockito.when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString()))
				.thenReturn(Optional.of(GuestPlayerBuilder.aPlayer().build()))
				.thenReturn(Optional.of(guestWallet().build()));

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin()
				.withMode(Mode.demo)
				.withAuthToken(AuthorizationPresets.ACCESSTOKEN)
				.build();

		PlayerWrapper playerWrapper = apiService.sendAuth(IgpPresets.IGPCODE_RHINO, guestLogin);

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());
		expectedHeader.get().add("DemoWallet-API-Key", "apiKey");

		assertThat(playerWrapper.getAuthToken()).isEqualTo(guestLogin.getAuthToken());

		Mockito.verify(meshService, Mockito.times(0))
				.validateGuestLaunch(IgpPresets.IGPCODE_IGUANA, AuthorizationPresets.ACCESSTOKEN);

		Mockito.verify(endpoint).send(
				Mockito.eq("http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/player"),
				Mockito.eq(HttpMethod.POST),
				Mockito.eq(Optional.of(guestLogin)),
				Mockito.eq(Optional.of(GuestPlayer.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(demoWalletServiceName)
		);
	}

	@Test
	public void okAuthPlayerLogin()
	{
		PlayerLogin playerLogin = PlayerLoginBuilder.aPlayerLogin().build();
		PlayerWrapper wrapper = PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(aPlayer().build())
				.withWallet(aWallet().build())
				.build();
		Mockito.when(meshService.sendAuth(IgpPresets.IGPCODE_IGUANA,playerLogin)).thenReturn(wrapper);

		Mockito.when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString()))
				.thenReturn(Optional.of(guestWallet()
						.withBalance(new BigDecimal("2000.00"))
						.build()));

		PlayerWrapper newWrapper = apiService.sendAuth(IgpPresets.IGPCODE_IGUANA,playerLogin);

		assertThat(newWrapper).isEqualToComparingFieldByFieldRecursively(PlayerWrapperBuilder.aPlayerWrapper()
				.withPlayer(aPlayer().build())
				.withWallet(aWallet()
						.withBalance(new BigDecimal("2000.00"))
						.build())
				.build());
		Mockito.verify(meshService).sendAuth(IgpPresets.IGPCODE_IGUANA,playerLogin);
	}

	@Test
	public void okGetPlayer()
	{
		Player player = aPlayer().build();

		Mockito.when(playerRepository.findByPlayerIdAndIgpCode(Mockito.any(), Mockito.any())).thenReturn(player);

		Player receivedPlayer = apiService.getPlayer(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID);

		Mockito.verify(playerRepository).findByPlayerIdAndIgpCode(PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

		assertThat(receivedPlayer).isEqualTo(player);
	}

	@Test(expected=NotFoundException.class)
	public void failureGetPlayerNotFound()
	{
		apiService.getPlayer(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID);
	}

	@Test
	public void getWallet()
	{
		GuestWallet guestWallet = guestWallet().build();

		Mockito.when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
		.thenReturn(Optional.of(guestWallet));

		Wallet wallet = apiService.getWallet(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, GamePresets.CODE);

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());

		expectedHeader.get().add("DemoWallet-API-Key", "apiKey");
		Mockito.verify(endpoint).send(
				Mockito.eq("http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/wallet?playerId={playerId}&igpCode={igpCode}&gameCode={gameCode}"),
				Mockito.eq(HttpMethod.GET),
				Mockito.eq(Optional.empty()),
				Mockito.eq(Optional.of(GuestWallet.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(demoWalletServiceName),
				Mockito.eq(PlayerPresets.PLAYERID),
				Mockito.eq(IgpPresets.IGPCODE_IGUANA),
				Mockito.eq(GamePresets.CODE));

		assertThat(wallet.getBalance()).isEqualTo(guestWallet.getBalance());

		Fund expectedFund = new BalanceFund(FundType.CASH, guestWallet.getBalance());
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

		TxnReceipt receivedReceipt = apiService.sendTxn(TxnBuilder.txn().build());

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());

		expectedHeader.get().add("DemoWallet-API-Key", "apiKey");
		Mockito.verify(endpoint).send(
				Mockito.eq("http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/txn"),
				Mockito.eq(HttpMethod.POST),
				Mockito.any(),
				Mockito.eq(Optional.of(TxnReceipt.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(demoWalletServiceName));

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

		apiService.cancelTxn(TxnBuilder.txn().build(), TxnCancelBuilder.txnCancel().build());

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());

		expectedHeader.get().add("DemoWallet-API-Key", "apiKey");
		Mockito.verify(endpoint).send(
				Mockito.eq("http://{demoWalletServiceName}/hive/s2s/platform/demowallet/v1/txn/{txnId}/cancel"),//demowallet uses internal id type
				Mockito.eq(HttpMethod.POST),
				Mockito.any(),
				Mockito.eq(Optional.of(TxnReceipt.class)),
				Mockito.eq(expectedHeader),
				Mockito.eq(demoWalletServiceName),
				Mockito.eq("1000-1"));
	}
}
