package io.gsi.hive.platform.player.mesh.gateway;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.mesh.igpservicelocator.SupportedIgpCodes;
import io.gsi.hive.platform.player.mesh.player.MeshPlayer;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuth;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerWrapper;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxn;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnCancel;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Loggable
@Service
@ConditionalOnProperty(
		value = "hive.player.mesh.gateway",
		havingValue = "legacy")
public class LegacyMeshGateway implements MeshGateway {

	private static final String RGS_CODE = "hive";

	private final MeshEndpoint endpoint;
	private final String meshApiKey;
	private final String baseUrl;
	private final boolean preferLegacyAuth;

	@Autowired
	public LegacyMeshGateway(MeshEndpoint endpoint,
							 @Value("${hive.mesh.meshApiKey}") String meshApiKey,
							 @Value("${hive.mesh.meshBaseUrl}") String baseUrl,
							 @Value("${hive.player.login.preferLegacyAuth:true}") boolean preferLegacyAuth) {
		this.endpoint = endpoint;
		this.meshApiKey = meshApiKey;
		this.baseUrl = baseUrl;
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
		final var authUrl = baseUrl + "/mesh/b2b/rgs/{RGS_CODE}/player/{playerId}/"
				+ "auth?rgsGameId={rgsGameId}&igpCode={igpCode}";
		return endpoint.send(
				authUrl,
				HttpMethod.POST,
				Optional.of(client),
				Optional.of(MeshPlayerWrapper.class),
				getMeshHeaders(playerAuth),
				RGS_CODE,
				playerId,
				rgsGameId,
				igpCode
		).get();
	}

	private MeshPlayerWrapper sendAuthenticate(String playerId, MeshPlayerAuth playerAuth,
			String rgsGameId, MeshPlayerClient client, String igpCode) {
		var authUrl = baseUrl + "/mesh/b2b/rgs/{RGS_CODE}/player/"
				+ "auth?rgsGameId={rgsGameId}&igpCode={igpCode}";
		if (playerId != null) {
			authUrl += "&playerId={playerId}";
		}
		return endpoint.send(
				authUrl,
				HttpMethod.POST,
				Optional.of(client),
				Optional.of(MeshPlayerWrapper.class),
				getMeshHeaders(playerAuth),
				RGS_CODE,
				rgsGameId,
				igpCode,
				playerId
		).get();
	}

	@Override
	public void validateGuestLaunch(String igpCode, String authToken) {
		endpoint.send(
				baseUrl + "/mesh/b2b/rgs/{rgsCode}/guest/validateLaunch?igpCode={igpCode}&authToken={authToken}",
				HttpMethod.POST,
				Optional.empty(),
				Optional.empty(),
				getMeshKeyHeader(),
				RGS_CODE,
				igpCode,
				authToken
		);
	}

	@Override
	public MeshPlayer getPlayer(String playerId, String igpCode) {
		return endpoint.send(
				baseUrl + "/mesh/b2b/rgs/{RGS_CODE}/player/{playerId}",
				HttpMethod.GET,
				Optional.empty(),
				Optional.of(MeshPlayer.class),
				getMeshKeyHeader(),
				RGS_CODE,
				playerId
		).get();
	}

	@Override
	public SupportedIgpCodes getSupportedIgpCodes(String igpCode) {
		throw new UnsupportedOperationException("Legacy mesh gateway getSupportedIgpCodes");
	}

	@Override
	public MeshWallet getWallet(String playerId, String rgsGameId, String igpCode,
								MeshPlayerAuth playerAuth) {
		return endpoint.send(
				baseUrl + "/mesh/b2b/rgs/{RGS_CODE}/player/{playerId}/wallet?rgsGameId={rgsGameId}&igpCode={igpCode}",
				HttpMethod.GET,
				Optional.empty(),
				Optional.of(MeshWallet.class),
				getMeshHeaders(playerAuth),
				RGS_CODE,
				playerId,
				rgsGameId,
				igpCode
		).get();
	}

	@Override
	public MeshGameTxnStatus processTxn(MeshPlayerAuth playerAuth, MeshGameTxn txn, String igpCode) {
		Optional<HttpHeaders> authHeader = getMeshKeyHeader();

		if (playerAuth.getToken() != null) {
			authHeader.get().add("Authorization", playerAuth.getHeader());
		}

		return endpoint.send(
				baseUrl+"/mesh/b2b/rgs/{RGS_CODE}/txn/?igpCode={igpCode}",
				HttpMethod.POST,
				Optional.of(txn),
				Optional.of(MeshGameTxnStatus.class),
				authHeader,
				RGS_CODE,
				igpCode
		).get();
	}

	@Override
	public MeshGameTxnStatus cancelTxn(String rgsTxnId, MeshGameTxnCancel txnCancel,
			String igpCode, MeshPlayerAuth meshPlayerAuth) {

		Optional<HttpHeaders> cancelHeaders = getMeshHeaders(meshPlayerAuth);

		return endpoint.send(
				baseUrl+"/mesh/b2b/rgs/{RGS_CODE}/txn/{rgsTxnId}/cancel/?igpCode={igpCode}",
				HttpMethod.POST,
				Optional.of(txnCancel),
				Optional.of(MeshGameTxnStatus.class),
				cancelHeaders,
				RGS_CODE,
				rgsTxnId,
				igpCode
		).get();
	}

	private Optional<HttpHeaders> getMeshKeyHeader() {
		HttpHeaders header =  new HttpHeaders();
		header.add("Mesh-API-Key", meshApiKey);
		return Optional.of(header);
	}

	private Optional<HttpHeaders> getMeshHeaders(MeshPlayerAuth playerAuth) {
		Optional<HttpHeaders> headers = getMeshKeyHeader();
		headers.get().add("Authorization", playerAuth.getHeader());
		return headers;
	}
}
