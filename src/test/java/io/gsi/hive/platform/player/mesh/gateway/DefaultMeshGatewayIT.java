package io.gsi.hive.platform.player.mesh.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.exception.AuthorizationException;
import io.gsi.commons.exception.BadRequestException;
import io.gsi.hive.platform.player.HivePlayer;
import io.gsi.hive.platform.player.builders.TxnBuilder;
import io.gsi.hive.platform.player.builders.TxnCancelBuilder;
import io.gsi.hive.platform.player.exception.ApiUnexpectedException;
import io.gsi.hive.platform.player.exception.ExceptionResponse;
import io.gsi.hive.platform.player.exception.MandatoryGameBreakException;
import io.gsi.hive.platform.player.exception.PlayerLimitException;
import io.gsi.hive.platform.player.mesh.igpservicelocator.DefaultIgpServiceLocator;
import io.gsi.hive.platform.player.mesh.mapping.MeshHiveMapping;
import io.gsi.hive.platform.player.mesh.player.MeshPlayer;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuth;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuthBuilder;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerBuilder;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnBuilder;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnCancel;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus.Status;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatusBuilder;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;
import io.gsi.hive.platform.player.mesh.wallet.MeshWalletBuilder;
import io.gsi.hive.platform.player.mesh.wallet.MeshWalletOperatorFreeroundsFund;
import io.gsi.hive.platform.player.presets.AuthorizationPresets;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.IgpPresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

import static io.gsi.hive.platform.player.mesh.wallet.MeshWalletFundPresets.getMeshWalletOperatorFreeRoundsFund;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * All of the internal functionality of MeshApiClient and Gateway are covered in MeshApiServiceIT
 * This test just ensures that the endpoint is properly configured
 * */

@RunWith(SpringRunner.class)
@TestPropertySource(value="/config/test.properties",properties = {
        "hive.player.mesh.gateway=default"
})
@SpringBootTest(classes={HivePlayer.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class DefaultMeshGatewayIT {

    private MockRestServiceServer mockRestServiceServer;

    @Autowired
    @Qualifier("meshRestTemplate")
    private RestTemplate restTemplate;
    @Autowired
    private DefaultMeshGateway defaultMeshGateway;
    @Autowired
    private MeshHiveMapping meshHiveMapping;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DefaultIgpServiceLocator igpServiceLocator;
    @MockBean
    private DiscoveryClient discoveryClient;

    @Value("${endpoint.mesh.N2NKey}")
    private String n2nKey;

    private static final String MESH_N2N_KEY = "Mesh-N2N-Key";
    private static final String AUTHORIZATION = "Authorization";

    @Before
    public void setupMockRestService() {
        this.mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
        when(igpServiceLocator.getServiceCode(IgpPresets.IGPCODE_IGUANA)).thenReturn(IgpPresets.IGPCODE_IGUANA);
        when(discoveryClient.getServices()).thenReturn(Arrays.asList("mesh-node-igp-iguana"));
    }

    @Test
    public void givenGuestLaunch_whenValidateGuestLaunch_returnOk() {
        mockRestServiceServer.expect(requestTo(
                URLPresets.getValidateGuestLaunchURL(IgpPresets.IGPCODE_IGUANA, AuthorizationPresets.ACCESSTOKEN)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess());

        this.defaultMeshGateway.validateGuestLaunch(IgpPresets.IGPCODE_IGUANA, AuthorizationPresets.ACCESSTOKEN);

        mockRestServiceServer.verify();
    }

    @Test
    public void givenGuestLaunch_whenValidateGuestLaunch_returnAuthorizationException() throws JsonProcessingException {
        final Map<String, Object> extraInfo = Map.of("Name", "value");

        final ExceptionResponse exceptionResponse = new ExceptionResponse("AuthorizationException", "break", "break", "1", extraInfo);
        String jsonException = objectMapper.writeValueAsString(exceptionResponse);

        mockRestServiceServer.expect(requestTo(
                URLPresets.getValidateGuestLaunchURL(IgpPresets.IGPCODE_IGUANA, AuthorizationPresets.ACCESSTOKEN)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED)
                        .body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

        assertThatThrownBy(() -> this.defaultMeshGateway.validateGuestLaunch(IgpPresets.IGPCODE_IGUANA, AuthorizationPresets.ACCESSTOKEN))
                .isEqualToComparingFieldByField(new AuthorizationException("break", extraInfo));

        mockRestServiceServer.verify();
    }

    @Test
    public void okGetPlayer() throws JsonProcessingException {

        MeshPlayer player = new MeshPlayerBuilder().get();
        String JsonPlayer = objectMapper.writeValueAsString(player);

        mockRestServiceServer.expect(requestTo(URLPresets.getPlayerUrl(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID)))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(JsonPlayer, MediaType.APPLICATION_JSON_UTF8));

        MeshPlayer receivedPlayer = defaultMeshGateway.getPlayer(PlayerPresets.PLAYERID, IgpPresets.IGPCODE_IGUANA);

        assertThat(player).isEqualTo(receivedPlayer);

        mockRestServiceServer.verify();
    }

    @Test
    public void okGetWallet() throws JsonProcessingException {

        MeshWallet wallet = new MeshWalletBuilder().get();
        String JsonPlayer = objectMapper.writeValueAsString(wallet);

        mockRestServiceServer.expect(requestTo(
                URLPresets.getWalletUrl(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, "testGame")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess(JsonPlayer, MediaType.APPLICATION_JSON_UTF8));

        MeshWallet receivedPlayer = defaultMeshGateway.getWallet(PlayerPresets.PLAYERID, GamePresets.CODE, IgpPresets.IGPCODE_IGUANA, new MeshPlayerAuthBuilder().get());

        assertThat(wallet).isEqualTo(receivedPlayer);

        mockRestServiceServer.verify();
    }


    @Test
    public void okSendTxn() throws JsonProcessingException {

        MeshGameTxnStatus meshGameTxnStatus = new MeshGameTxnStatusBuilder().get();
        String JsonTxnStatus = objectMapper.writeValueAsString(meshGameTxnStatus);

        mockRestServiceServer.expect(requestTo(URLPresets.processTxnUrl(IgpPresets.IGPCODE_IGUANA)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(withSuccess(JsonTxnStatus, MediaType.APPLICATION_JSON_UTF8));

        MeshGameTxnStatus receivedStatus = defaultMeshGateway.processTxn(new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN), new MeshGameTxnBuilder().get(), IgpPresets.IGPCODE_IGUANA);

        assertThat(meshGameTxnStatus).isEqualTo(receivedStatus);

        mockRestServiceServer.verify();
    }

    @Test
    public void okCancelTxn() throws JsonProcessingException {

        MeshGameTxnStatus meshGameTxnStatus = new MeshGameTxnStatusBuilder().get();
        String JsonTxnStatus = objectMapper.writeValueAsString(meshGameTxnStatus);

        MeshGameTxnStatus meshGameTxnCancelStatus = new MeshGameTxnStatusBuilder().withStatus(Status.CANCELLED).get();
        String JsonTxnCancelStatus = objectMapper.writeValueAsString(meshGameTxnCancelStatus);

        //txn
        mockRestServiceServer.expect(requestTo(URLPresets.processTxnUrl(IgpPresets.IGPCODE_IGUANA)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(withSuccess(JsonTxnStatus, MediaType.APPLICATION_JSON_UTF8));

        //cancel
        mockRestServiceServer.expect(requestTo(URLPresets.cancelTxnUrl(IgpPresets.IGPCODE_IGUANA, "1")))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(withSuccess(JsonTxnCancelStatus, MediaType.APPLICATION_JSON_UTF8));

        MeshGameTxnStatus recievedStatus = defaultMeshGateway.processTxn(new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN), new MeshGameTxnBuilder().get(), IgpPresets.IGPCODE_IGUANA);

        assertThat(meshGameTxnStatus).isEqualTo(recievedStatus);

        MeshGameTxnCancel meshCancel = meshHiveMapping.hiveToMeshTxnCancel(TxnBuilder.txn().build(), TxnCancelBuilder.txnCancel().build());
        recievedStatus = defaultMeshGateway.cancelTxn(meshGameTxnStatus.getIgpTxnId(), meshCancel, IgpPresets.IGPCODE_IGUANA, new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN));

        assertThat(meshGameTxnCancelStatus).isEqualTo(recievedStatus);

        mockRestServiceServer.verify();
    }

    @Test
    public void failureSendTxnErrorResponse() throws JsonProcessingException {

        ExceptionResponse exceptionResponse = new ExceptionResponse("PlayerLimitException", "limit", "limit", "1", null);
        String jsonException = objectMapper.writeValueAsString(exceptionResponse);

        mockRestServiceServer.expect(requestTo(URLPresets.processTxnUrl(IgpPresets.IGPCODE_IGUANA)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CONFLICT).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

        assertThatThrownBy(() -> defaultMeshGateway.processTxn(
                new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN),
                new MeshGameTxnBuilder().get(),
                IgpPresets.IGPCODE_IGUANA)
        ).isEqualToComparingFieldByField(new PlayerLimitException("limit"));

        mockRestServiceServer.verify();
    }

    @Test
    public void failureSendTxnMandatoryGameBreakResponse() throws JsonProcessingException {

        ExceptionResponse exceptionResponse = new ExceptionResponse("MandatoryGameBreakException", "break", "break", "1", null);
        String jsonException = objectMapper.writeValueAsString(exceptionResponse);

        mockRestServiceServer.expect(requestTo(URLPresets.processTxnUrl(IgpPresets.IGPCODE_IGUANA)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

        assertThatThrownBy(() -> defaultMeshGateway.processTxn(new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN), new MeshGameTxnBuilder().get(), IgpPresets.IGPCODE_IGUANA)).isEqualToComparingFieldByField(new MandatoryGameBreakException("break"));

        mockRestServiceServer.verify();
    }

    @Test
    public void sendExtraInfoInException() throws JsonProcessingException {
        final var extraInfo = Map.of("Name", (Object) "value");
        final var exceptionResponse = new ExceptionResponse("AuthorizationException", "break", "break", "1", extraInfo);
        String jsonException = objectMapper.writeValueAsString(exceptionResponse);

        mockRestServiceServer.expect(requestTo(URLPresets.processTxnUrl(IgpPresets.IGPCODE_IGUANA)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED).body(jsonException).contentType(MediaType.APPLICATION_JSON_UTF8));

        assertThatThrownBy(() -> defaultMeshGateway.processTxn(new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN), new MeshGameTxnBuilder().get(), IgpPresets.IGPCODE_IGUANA))
                .isEqualToComparingFieldByField(new AuthorizationException("break", extraInfo));

        mockRestServiceServer.verify();
    }

    @Test
    public void okGetWalletWithOperatorFreeroundsFund() throws JsonProcessingException {
        MeshWalletOperatorFreeroundsFund meshWalletOperatorFreeRoundsFund =
                getMeshWalletOperatorFreeRoundsFund();
        MeshWallet wallet = new MeshWalletBuilder().withFunds(meshWalletOperatorFreeRoundsFund).get();
        String JsonPlayer = objectMapper.writeValueAsString(wallet);

        mockRestServiceServer.expect(requestTo(
                URLPresets.getWalletUrl(IgpPresets.IGPCODE_IGUANA, PlayerPresets.PLAYERID, "testGame")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess(JsonPlayer, MediaType.APPLICATION_JSON_UTF8));

        MeshWallet receivedPlayer = defaultMeshGateway.getWallet(PlayerPresets.PLAYERID, GamePresets.CODE, IgpPresets.IGPCODE_IGUANA, new MeshPlayerAuthBuilder().get());

        assertThat(wallet).isEqualTo(receivedPlayer);

        mockRestServiceServer.verify();
    }

    @Test
    public void givenBadRequestExceptionResponse_whenSendTxn_thenBadRequestException() throws JsonProcessingException {
        ExceptionResponse exceptionResponse = new ExceptionResponse("BadRequestException", "break",
                "break", "1", null);
        String jsonException = objectMapper.writeValueAsString(exceptionResponse);

        mockRestServiceServer.expect(requestTo(URLPresets.processTxnUrl(IgpPresets.IGPCODE_IGUANA)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST).body(jsonException)
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        assertThatThrownBy(() -> defaultMeshGateway.processTxn(new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN),
                new MeshGameTxnBuilder().get(), IgpPresets.IGPCODE_IGUANA))
                .as("GSI BadRequestException has been thrown")
                .isEqualToComparingFieldByField(new BadRequestException("break"));

        mockRestServiceServer.verify();
    }

    @Test
    public void givenConstraintViolationResponse_whenSendTxn_thenApiUnexpectedException() throws JsonProcessingException {
        ExceptionResponse exceptionResponse = new ExceptionResponse("ConstraintViolationException", "break",
                "break", "1", null);
        String jsonException = objectMapper.writeValueAsString(exceptionResponse);

        mockRestServiceServer.expect(requestTo(URLPresets.processTxnUrl(IgpPresets.IGPCODE_IGUANA)))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(MESH_N2N_KEY, n2nKey))
                .andExpect(header(AUTHORIZATION, "Bearer testToken"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST).body(jsonException)
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        assertThatThrownBy(() -> defaultMeshGateway.processTxn(new MeshPlayerAuth(AuthorizationPresets.ACCESSTOKEN),
                new MeshGameTxnBuilder().get(), IgpPresets.IGPCODE_IGUANA))
                .as("GSI ApiUnexpectedException has been thrown")
                .isEqualToComparingFieldByField(new ApiUnexpectedException("break"));

        mockRestServiceServer.verify();
    }
}