package io.gsi.hive.platform.player.mesh.gateway;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.hive.platform.player.HivePlayer;
import io.gsi.hive.platform.player.bonus.builders.FreeroundsFundBuilder;
import io.gsi.hive.platform.player.builders.GuestLoginBuilder;
import io.gsi.hive.platform.player.builders.PlayerLoginBuilder;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.mesh.MeshService;
import io.gsi.hive.platform.player.mesh.igpservicelocator.DefaultIgpServiceLocator;
import io.gsi.hive.platform.player.mesh.mapping.MeshHiveMapping;
import io.gsi.hive.platform.player.mesh.player.*;
import io.gsi.hive.platform.player.mesh.presets.MeshAuthorizationPresets;
import io.gsi.hive.platform.player.mesh.presets.MeshPlayerIdPresets;
import io.gsi.hive.platform.player.mesh.txn.*;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus.Status;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;
import io.gsi.hive.platform.player.mesh.wallet.MeshWalletBuilder;
import io.gsi.hive.platform.player.mesh.wallet.MeshWalletFundPresets;
import io.gsi.hive.platform.player.presets.*;
import io.gsi.hive.platform.player.session.GuestLogin;
import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.BonusFundDetailsPresets;
import io.gsi.hive.platform.player.txn.TxnCancel;
import io.gsi.hive.platform.player.txn.TxnType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static io.gsi.hive.platform.player.presets.TimePresets.EXPECTED_STAKE_TXN_DEADLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource("/config/test.properties")
@SpringBootTest(classes={HivePlayer.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class MeshServiceIT {

	@Autowired
	private MeshService apiService;
	@Autowired
	private DefaultMeshGateway defaultMeshGateway;
	@Autowired
	private MeshHiveMapping meshHiveMapping;
	@MockBean
	private MeshEndpoint endpoint;
	@MockBean
	private DefaultIgpServiceLocator igpServiceLocator;

	@Value("${endpoint.mesh.N2NKey}")
	private String n2nKey;

	@Before
	public void setup() {
		defaultMeshGateway.setPreferLegacyAuth(false);
		when(igpServiceLocator.getServiceCode(IgpPresets.IGPCODE_IGUANA)).thenReturn(IgpPresets.IGPCODE_IGUANA);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Mesh-N2N-Key", n2nKey);
		when(endpoint.addHeaders(Mockito.any())).thenReturn(httpHeaders);
	}

	@Test
	public void okAuth() {
		sendAuth(PlayerPresets.PLAYERID);

		verifySendAuth("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/player/auth?" +
				"rgsCode={rgsCode}&rgsGameId={rgsGameId}&playerId={playerId}", PlayerPresets.PLAYERID);
	}

	@Test
	public void givenValidGuestLoginWithoutAuth_whenValidateGuestLaunch_returnOk() {
		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());
		expectedHeader.get().add("Mesh-API-Key", "apiKey");

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin()
				.withMode(Mode.demo)
				.build();

		apiService.validateGuestLaunch(IgpPresets.IGPCODE_IGUANA, guestLogin.getAuthToken());

		Mockito.verify(endpoint, Mockito.times(1)).send(
				Mockito.eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/guest/validateLaunch?" +
						"authToken={authToken}"),
				Mockito.eq(HttpMethod.POST),
				Mockito.eq(Optional.empty()),
				Mockito.eq(Optional.empty()),
				Mockito.eq(Optional.empty()),
				Mockito.eq(IgpPresets.IGPCODE_IGUANA),
				Mockito.eq(IgpPresets.IGPCODE_IGUANA),
				Mockito.eq(null)
		);
	}

	@Test
	public void givenValidGuestLoginWithAuth_whenValidateGuestLaunch_returnOk() {
		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());
		expectedHeader.get().add("Mesh-API-Key", "apiKey");

		GuestLogin guestLogin = GuestLoginBuilder.aGuestLogin()
				.withMode(Mode.demo)
				.withAuthToken(AuthorizationPresets.ACCESSTOKEN)
				.build();

		apiService.validateGuestLaunch(IgpPresets.IGPCODE_IGUANA, guestLogin.getAuthToken());

		Mockito.verify(endpoint, Mockito.times(1)).send(
				Mockito.eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/guest/validateLaunch?" +
						"authToken={authToken}"),
				Mockito.eq(HttpMethod.POST),
				Mockito.eq(Optional.empty()),
				Mockito.eq(Optional.empty()),
				Mockito.eq(Optional.empty()),
				Mockito.eq(IgpPresets.IGPCODE_IGUANA),
				Mockito.eq(IgpPresets.IGPCODE_IGUANA),
				Mockito.eq(AuthorizationPresets.ACCESSTOKEN)
		);
	}

	@Test
	public void okAuthWithoutPlayerId() {
		sendAuth(null);
		verifySendAuth("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/player/auth?" +
				"rgsCode={rgsCode}&rgsGameId={rgsGameId}", null);
	}

	@Test
	public void okLegacyAuth() {
		defaultMeshGateway.setPreferLegacyAuth(true);
		sendAuth(PlayerPresets.PLAYERID);

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/player/{playerId}/auth?" +
						"rgsCode={rgsCode}&rgsGameId={rgsGameId}"),
				eq(HttpMethod.POST),
				eq(getExpectedClient()),
				eq(Optional.of(MeshPlayerWrapper.class)),
				eq(getExpectedHttpHeaders("Bearer testToken")),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq("player1"),
				eq("hive"),
				eq("testGame")
		);
	}

	@Test
	public void okAuthWithPreferLegacyAuthAndWithoutPlayerId() {
		defaultMeshGateway.setPreferLegacyAuth(true);
		sendAuth(null);

		verifySendAuth("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/player/auth?" +
				"rgsCode={rgsCode}&rgsGameId={rgsGameId}", null);
	}

	@Test(expected=UnsupportedOperationException.class)
	public void failureAuthGuestUnsupported() {
		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any()))
				.thenReturn(Optional.of(new MeshPlayerWrapperBuilder().get()));

		apiService.sendAuth(IgpPresets.IGPCODE_IGUANA, GuestLoginBuilder.aGuestLogin().build());
	}

	@Test
	public void okGetPlayer() {
		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
				.thenReturn(Optional.of(new MeshPlayerBuilder().get()));

		apiService.getPlayer(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID);

		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());
		expectedHeader.get().add("Mesh-API-Key", "apiKey");

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/player/{playerId}?rgsCode={rgsCode}"),
				eq(HttpMethod.GET),
				eq(Optional.empty()),
				eq(Optional.of(MeshPlayer.class)),
				eq(Optional.empty()),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(PlayerPresets.PLAYERID),
				eq("hive"));
	}

	@Test
	public void okGetWallet() {
		when(endpoint.send(
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
				.thenReturn(Optional.of(new MeshWalletBuilder().get()));

		apiService.getWallet(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, GamePresets.CODE, MeshAuthorizationPresets.DEFAULT_TOKEN);

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/player/{playerId}/wallet?rgsCode={rgsCode}&rgsGameId={rgsGameId}"),
				eq(HttpMethod.GET),
				eq(Optional.empty()),
				eq(Optional.of(MeshWallet.class)),
				eq(getExpectedHttpHeaders("Bearer token")),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(PlayerPresets.PLAYERID),
				eq("hive"),
				eq(GamePresets.CODE));
	}

	@Test
	public void okSendTxn() {
		ZonedDateTime expectedTxnDeadline = TimePresets.ZONEDEPOCHUTC.plusSeconds(29);

		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
				.thenReturn(Optional.of(new MeshGameTxnStatusBuilder().get()));

		apiService.sendTxn(IgpPresets.IGPCODE_IGUANA, TxnBuilder.txn()
				.withExtraInfo(Map.of("info", "test"))
				.build());

		MeshGameTxnAction action = new MeshGameTxnActionBuilder()
				.withRgsActionId("1000-1")
				.withJackpotAmount(new BigDecimal("10.00"))
				.get();

		MeshGameTxn expectedTxn = new MeshGameTxnBuilder()
				.withRgsTxnId("1000-1")
				.withRgsGameId("testGame")
				.withRoundComplete(true)
				.withPlayComplete(true)
				.withRgsRoundId("1000-10")
				.withRgsPlayId("1000-10")
				.withActions(action)
				.withExtraInfo(Map.of("info", "test"))
				.get();

		ArgumentCaptor<Optional<MeshGameTxn>> gameTxnCaptor = ArgumentCaptor.forClass(Optional.class);

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/txn?rgsCode={rgsCode}"),
				eq(HttpMethod.POST),
				gameTxnCaptor.capture(),
				eq(Optional.of(MeshGameTxnStatus.class)),
				eq(getExpectedHttpHeaders("Bearer testToken")),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq("hive"));

		MeshGameTxn actual = gameTxnCaptor.getValue().orElse(null);

		assertThat(actual).isNotNull();
		assertThat(actual.getTxnDeadline()).isNotNull();
		assertThat(actual.getTxnDeadline().isAfter(expectedTxnDeadline)).isTrue();
		assertThat(actual.getRgsTxnId()).isEqualTo(expectedTxn.getRgsTxnId());
		assertThat(actual.getRgsGameId()).isEqualTo(expectedTxn.getRgsGameId());
		assertThat(actual.getRoundComplete()).isEqualTo(expectedTxn.getRoundComplete());
		assertThat(actual.getPlayComplete()).isEqualTo(expectedTxn.getPlayComplete());
		assertThat(actual.getRgsRoundId()).isEqualTo(expectedTxn.getRgsRoundId());
		assertThat(actual.getRgsPlayId()).isEqualTo(expectedTxn.getRgsPlayId());
		assertThat(actual.getActions()).isEqualTo(expectedTxn.getActions());
		assertThat(actual.getExtraInfo()).isEqualTo(expectedTxn.getExtraInfo());
	}

	@Test
	public void okCancelTxn() {
		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
				.thenReturn(Optional.of(new MeshGameTxnStatusBuilder().withStatus(Status.CANCELLED).get()));

		apiService.cancelTxn(IgpPresets.IGPCODE_IGUANA, TxnBuilder.txn().build(), new TxnCancel());

		Optional<HttpHeaders> expectedHeader = getExpectedHttpHeaders(
				"Bearer " + AuthorizationPresets.ACCESSTOKEN);

		Optional<MeshGameTxnCancel> expectedTxnCancel = Optional.of(MeshGameTxnCancel.builder()
				.playerId(MeshPlayerIdPresets.DEFAULT)
				.rgsTxnCancelId(null)
				.roundComplete(false)
				.playComplete(false)
				.reason(null)
				.rgsGameId("testGame")
				.rgsRoundId("1000-10")
				.currency("GBP")
				.rgsPlayId("1000-10")
				.amount(new BigDecimal(20.00).setScale(2))
				.build());

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/txn/{rgsTxnId}/cancel?rgsCode={rgsCode}"),
				eq(HttpMethod.POST),
				eq(expectedTxnCancel),
				eq(Optional.of(MeshGameTxnStatus.class)),
				eq(expectedHeader),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq("1000-1"),
				eq("hive"));
	}

	@Test
	public void okSendFreeroundsWinTxn() {
		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
				.thenReturn(Optional.of(new MeshGameTxnStatusBuilder().get()));

		apiService.sendFreeroundsWinTxn(IgpPresets.IGPCODE_IGUANA, TxnBuilder.txn().withExtraInfo(Map.of("extra", "test")).build(), Boolean.TRUE,
				FreeroundsFundBuilder.freeroundsFund().build());

		MeshGameTxnAction action = new MeshGameTxnActionBuilder()
				.withRgsActionId("award1")
				.withType(MeshGameTxnActionType.RGS_FREEROUND_WIN)
				.get();

		Optional<MeshGameTxn> expectedTxn = Optional.of(new MeshGameTxnBuilder()
				.withRgsTxnId("1000-1")
				.withRgsGameId("testGame")
				.withRoundComplete(true)
				.withPlayComplete(true)
				.withRgsRoundId("1000-10")
				.withRgsPlayId("1000-10")
				.withActions(action)
				.withTxnDeadline(TimePresets.EXPECTED_STAKE_TXN_DEADLINE)
				.withExtraInfo(Map.of("extra", "test"))
				.get());

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/txn?rgsCode={rgsCode}"),
				eq(HttpMethod.POST),
				eq(expectedTxn),
				eq(Optional.of(MeshGameTxnStatus.class)),
				eq(getExpectedHttpHeaders("Bearer testToken")),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq("hive"));
	}

	@Test
	public void okSendFreeroundsCleardownTxn() {
		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
				.thenReturn(Optional.of(new MeshGameTxnStatusBuilder().get()));

		apiService.sendFreeroundsCleardownTxn(BigDecimal.TEN, IgpPresets.IGPCODE_IGUANA, TxnBuilder.txn()
				.withExtraInfo(Map.of("extra", "test")).build(), FreeroundsFundBuilder.freeroundsFund().build());

		MeshGameTxnAction action = new MeshGameTxnActionBuilder()
				.withRgsActionId("award1")
				.withType(MeshGameTxnActionType.RGS_FREEROUND_CLEARDOWN)
				.withAmount(BigDecimal.TEN)
				.get();

		Optional<MeshGameTxn> expectedTxn = Optional.of(new MeshGameTxnBuilder()
				.withRgsTxnId("1000-1")
				.withRgsGameId("testGame")
				.withRoundComplete(true)
				.withPlayComplete(true)
				.withRgsRoundId("1000-10")
				.withRgsPlayId("1000-10")
				.withActions(action)
				.withTxnDeadline(EXPECTED_STAKE_TXN_DEADLINE)
				.withExtraInfo(Map.of("extra", "test"))
				.get());

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/txn?rgsCode={rgsCode}"),
				eq(HttpMethod.POST),
				eq(expectedTxn),
				eq(Optional.of(MeshGameTxnStatus.class)),
				eq(getExpectedHttpHeaders("Bearer testToken")),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq("hive"));
	}

	@Test
	public void okSendOperatorFreeroundsTxn() {
		ZonedDateTime expectedTxnDeadline = TimePresets.ZONEDEPOCHUTC.plusSeconds(29);

		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
				.thenReturn(Optional.of(new MeshGameTxnStatusBuilder()
						.withWallet(
								new MeshWalletBuilder()
										.withFunds(MeshWalletFundPresets.getMeshWalletOperatorFreeRoundsFund())
										.get())
						.get()));

		apiService.sendOperatorFreeroundsTxn(IgpPresets.IGPCODE_IGUANA, TxnBuilder.txn()
				.withType(TxnType.OPFRSTK)
				.withExtraInfo(Map.of("extra", "test"))
				.build(), BonusFundDetailsPresets.defaultOperatorBonusFundDetails().build());

		MeshOperatorFreeroundGameTxnAction action = new MeshOperatorFreeroundGameTxnAction(
				MeshGameTxnActionType.OPERATOR_FREEROUND_STAKE,
				"1000-1",
				MonetaryPresets.BDAMOUNT,
				MonetaryPresets.BDHALFAMOUNT,
				WalletPresets.BONUS_ID,
				WalletPresets.AWARD_ID,
				WalletPresets.FREEROUNDS_REMAINING,
				WalletPresets.EXTRA_INFO);

		MeshGameTxn expectedTxn = new MeshGameTxnBuilder()
				.withRgsTxnId("1000-1")
				.withRgsGameId("testGame")
				.withRoundComplete(true)
				.withPlayComplete(true)
				.withRgsRoundId("1000-10")
				.withRgsPlayId("1000-10")
				.withActions(action)
				.withExtraInfo(Map.of("extra", "test"))
				.get();

		ArgumentCaptor<Optional<MeshGameTxn>> gameTxnCaptor = ArgumentCaptor.forClass(Optional.class);

		verify(endpoint).send(
				eq("http://mesh-node-igp-{igpCode}/mesh/n2n/igp/{igpCode}/txn?rgsCode={rgsCode}"),
				eq(HttpMethod.POST),
				gameTxnCaptor.capture(),
				eq(Optional.of(MeshGameTxnStatus.class)),
				eq(getExpectedHttpHeaders("Bearer testToken")),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq("hive"));

		MeshGameTxn actual = gameTxnCaptor.getValue().orElse(null);

		assertThat(actual).isNotNull();
		assertThat(actual.getTxnDeadline()).isNotNull();
		assertThat(actual.getTxnDeadline().isAfter(expectedTxnDeadline)).isTrue();
		assertThat(actual.getRgsTxnId()).isEqualTo(expectedTxn.getRgsTxnId());
		assertThat(actual.getRgsGameId()).isEqualTo(expectedTxn.getRgsGameId());
		assertThat(actual.getRoundComplete()).isEqualTo(expectedTxn.getRoundComplete());
		assertThat(actual.getPlayComplete()).isEqualTo(expectedTxn.getPlayComplete());
		assertThat(actual.getRgsRoundId()).isEqualTo(expectedTxn.getRgsRoundId());
		assertThat(actual.getRgsPlayId()).isEqualTo(expectedTxn.getRgsPlayId());
		assertThat(actual.getActions()).isEqualTo(expectedTxn.getActions());
		assertThat(actual.getExtraInfo()).isEqualTo(expectedTxn.getExtraInfo());
	}

	@Test(expected = AuthorizationException.class)
	public void cancelTxnWithNullTokenFails() {
		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString()))
				.thenReturn(Optional.of(new MeshGameTxnStatusBuilder().withStatus(Status.CANCELLED).get()));

		apiService.cancelTxn(IgpPresets.IGPCODE_IGUANA, TxnBuilder.txn().withAccessToken(null).build(), new TxnCancel());
	}

	private void verifySendAuth(final String authUrl, final String playerId) {
		verify(endpoint).send(
				eq(authUrl),
				eq(HttpMethod.POST),
				eq(getExpectedClient()),
				eq(Optional.of(MeshPlayerWrapper.class)),
				eq(getExpectedHttpHeaders("Bearer testToken")),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq(IgpPresets.IGPCODE_IGUANA),
				eq("hive"),
				eq("testGame"),
				eq(playerId)
		);
	}

	private void sendAuth(final String playerId) {
		when(endpoint.send(
				Mockito.anyString(),
				Mockito.any(),
				Mockito.any(),
				Mockito.any(Optional.class),
				Mockito.any(),
				Mockito.any()))
				.thenReturn(Optional.of(new MeshPlayerWrapperBuilder().get()));

		apiService.sendAuth(IgpPresets.IGPCODE_IGUANA,
				PlayerLoginBuilder.aPlayerLogin().withPlayerId(playerId).build());
	}

	private Optional<HttpHeaders> getExpectedHttpHeaders(String s) {
		Optional<HttpHeaders> expectedHeader = Optional.of(new HttpHeaders());
		expectedHeader.get().add("Mesh-N2N-Key", n2nKey);
		expectedHeader.get().add("Authorization", s);
		return expectedHeader;
	}

	private Optional<MeshPlayerClient> getExpectedClient() {
		return Optional.of(new MeshPlayerClientBuilder().get());
	}
}
