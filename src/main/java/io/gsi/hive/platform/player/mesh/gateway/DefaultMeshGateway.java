package io.gsi.hive.platform.player.mesh.gateway;

import io.gsi.commons.logging.Loggable;
import io.gsi.commons.monitoring.ExceptionMetered;
import io.gsi.hive.platform.player.mesh.igpservicelocator.DefaultIgpServiceLocator;
import io.gsi.hive.platform.player.mesh.igpservicelocator.IgpServiceLocator;
import io.gsi.hive.platform.player.mesh.igpservicelocator.SupportedIgpCodes;
import io.gsi.hive.platform.player.mesh.player.MeshPlayer;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuth;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerWrapper;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxn;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnCancel;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;
import io.gsi.hive.platform.player.validation.ValidationService;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Loggable
@Service
@ConditionalOnProperty(
		value="hive.player.mesh.gateway",
		havingValue = "default",
		matchIfMissing = true)
public class DefaultMeshGateway implements MeshGateway {

	private static final String RGS_CODE = "hive";

	private boolean preferLegacyAuth;

	private final MeshEndpoint endpoint;
	private final ValidationService validationService;

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String MESH_N2N_URL = "http://mesh-node-igp-{igpCode}/mesh/n2n";
	private static final String OPERATOR_URL = MESH_N2N_URL + "/igp/{igpCode}";
	private static final String PLAYER_URL = OPERATOR_URL + "/player";

	private static final String RGS_CODE_REQUEST_PARAM = "rgsCode={rgsCode}";
	private static final String RGS_GAME_ID_REQUEST_PARAM = "rgsGameId={rgsGameId}";
	private static final String PLAYER_ID_REQUEST_PARAM = "playerId={playerId}";
	private static final String AMPERSAND = "&";

	private final IgpServiceLocator igpServiceLocator;

	public DefaultMeshGateway(@Value("${hive.player.login.preferLegacyAuth:true}") boolean preferLegacyAuth,
			@Lazy DefaultIgpServiceLocator defaultIgpServiceLocator,
			ValidationService validationService, MeshEndpoint endpoint) {
		setPreferLegacyAuth(preferLegacyAuth);
		this.igpServiceLocator = defaultIgpServiceLocator;
		this.validationService = validationService;
		this.endpoint = endpoint;
	}

	public void setPreferLegacyAuth(boolean preferLegacyAuth) {
		this.preferLegacyAuth = preferLegacyAuth;
	}

	@Override
	public MeshPlayerWrapper authenticate(String playerId, MeshPlayerAuth playerAuth,
										  String rgsGameId, MeshPlayerClient client, String igpCode) {
		if (StringUtils.isNotBlank(playerId) && preferLegacyAuth) {
			return sendLegacyAuthenticate(playerId, playerAuth, rgsGameId, client, igpCode);
		}
		return sendAuthenticate(playerId, playerAuth, rgsGameId, client, igpCode);
	}

	private MeshPlayerWrapper sendLegacyAuthenticate(String playerId, MeshPlayerAuth playerAuth,
			String rgsGameId, MeshPlayerClient client, String igpCode) {

		var authUrl = new StringBuilder(PLAYER_URL)
				.append("/{playerId}/auth?")
				.append(RGS_CODE_REQUEST_PARAM);

		if (StringUtils.isNotBlank(rgsGameId)) {
			authUrl.append(AMPERSAND).append(RGS_GAME_ID_REQUEST_PARAM);
		}

		return endpoint.send(
				authUrl.toString(),
				HttpMethod.POST,
				Optional.of(client),
				Optional.of(MeshPlayerWrapper.class),
				getMeshHeaders(playerAuth, HttpMethod.POST),
				igpServiceLocator.getServiceCode(igpCode),
				igpCode,
				playerId,
				RGS_CODE,
				rgsGameId)
				.get();
	}

	private MeshPlayerWrapper sendAuthenticate(String playerId, MeshPlayerAuth playerAuth,
			String rgsGameId, MeshPlayerClient client, String igpCode) {
		var authUrl = new StringBuilder(PLAYER_URL)
				.append("/auth?")
				.append(RGS_CODE_REQUEST_PARAM);

		if (StringUtils.isNotBlank(rgsGameId)) {
			authUrl.append(AMPERSAND).append(RGS_GAME_ID_REQUEST_PARAM);
		}

		if (StringUtils.isNotBlank(playerId)) {
			authUrl.append(AMPERSAND).append(PLAYER_ID_REQUEST_PARAM);
		}

		return endpoint.send(
				authUrl.toString(),
				HttpMethod.POST,
				Optional.of(client),
				Optional.of(MeshPlayerWrapper.class),
				getMeshHeaders(playerAuth, HttpMethod.POST),
				igpServiceLocator.getServiceCode(igpCode),
				igpCode,
				RGS_CODE,
				rgsGameId,
				playerId)
				.get();
	}

	@Override
	public void validateGuestLaunch(String igpCode, String authToken) {
		endpoint.send(
				OPERATOR_URL + "/guest/validateLaunch?authToken={authToken}",
				HttpMethod.POST,
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				igpServiceLocator.getServiceCode(igpCode),
				igpCode,
				authToken);
	}

	@Timed
	@ExceptionMetered
	@Override
	public MeshPlayer getPlayer(String playerId, String igpCode) {
		return endpoint.send(
				PLAYER_URL + "/{playerId}?" + RGS_CODE_REQUEST_PARAM,
				HttpMethod.GET,
				Optional.empty(),
				Optional.of(MeshPlayer.class),
				Optional.empty(),
				igpServiceLocator.getServiceCode(igpCode),
				igpCode,
				playerId,
				RGS_CODE)
				.get();
	}

	@Timed
	@ExceptionMetered
	@Override
	public MeshWallet getWallet(String playerId, String rgsGameId, String igpCode, MeshPlayerAuth playerAuth) {
		//playerAuth is optional on findWallet
		Optional<HttpHeaders> httpHeaders = Optional.empty();
		if (playerAuth != null) {
			validationService.validate(playerAuth);
			httpHeaders = getMeshHeaders(playerAuth, HttpMethod.GET);
		}

		var url = new StringBuilder(PLAYER_URL)
				.append("/{playerId}/wallet?")
				.append(RGS_CODE_REQUEST_PARAM);
		if (rgsGameId != null && rgsGameId.length() != 0) {
			url.append(AMPERSAND).append(RGS_GAME_ID_REQUEST_PARAM);
		}

		return endpoint.send(
				url.toString(),
				HttpMethod.GET,
				Optional.empty(),
				Optional.of(MeshWallet.class),
				httpHeaders,
				igpServiceLocator.getServiceCode(igpCode),
				igpCode,
				playerId,
				RGS_CODE,
				rgsGameId)
				.get();
	}

	@Timed
	@ExceptionMetered
	@Override
	public MeshGameTxnStatus processTxn(MeshPlayerAuth playerAuth, MeshGameTxn txn, String igpCode) {
		validationService.validate(playerAuth);
		validationService.validate(txn);

		return endpoint.send(
				OPERATOR_URL + "/txn?" + RGS_CODE_REQUEST_PARAM,
				HttpMethod.POST,
				Optional.of(txn),
				Optional.of(MeshGameTxnStatus.class),
				getMeshHeaders(playerAuth, HttpMethod.POST),
				igpServiceLocator.getServiceCode(igpCode),
				igpCode,
				RGS_CODE)
				.get();
	}

	@Timed
	@ExceptionMetered
	@Override
	public MeshGameTxnStatus cancelTxn(String rgsTxnId, MeshGameTxnCancel txnCancel, String igpCode, MeshPlayerAuth playerAuth) {
		validationService.validate(txnCancel);
		return endpoint.send(
				OPERATOR_URL + "/txn/{rgsTxnId}/cancel?" + RGS_CODE_REQUEST_PARAM,
				HttpMethod.POST,
				Optional.of(txnCancel),
				Optional.of(MeshGameTxnStatus.class),
				getMeshHeaders(playerAuth, HttpMethod.POST),
				igpServiceLocator.getServiceCode(igpCode),
				igpCode,
				rgsTxnId,
				RGS_CODE)
				.get();
	}

	@Override
	public SupportedIgpCodes getSupportedIgpCodes(String igpCode) {
		return endpoint
				.send(
						MESH_N2N_URL + "/igpCodes",
						HttpMethod.GET,
						Optional.empty(),
						Optional.of(SupportedIgpCodes.class),
						Optional.empty(),
						igpCode)
				.get();
	}

	private Optional<HttpHeaders> getMeshHeaders(MeshPlayerAuth playerAuth, HttpMethod httpMethod) {
		HttpHeaders headers = endpoint.addHeaders(httpMethod);
		headers.add(AUTHORIZATION_HEADER, playerAuth.getHeader());
		return Optional.of(headers);
	}
}
