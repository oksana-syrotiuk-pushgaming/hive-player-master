/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.txn;

import com.jayway.jsonpath.PathNotFoundException;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.mesh.presets.MeshAuthorizationPresets;
import io.gsi.hive.platform.player.mesh.presets.MeshIgpPlayIdPresets;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus.Status;
import io.gsi.hive.platform.player.mesh.wallet.MeshWalletType;

import java.util.Arrays;
import org.junit.Test;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MeshGameTxnStatusTests extends DomainTestBase {

	@Test
	public void successfulSerialization() {
		MeshGameTxnStatus txnStatus = new MeshGameTxnStatusBuilder().get();
		String json = jsonMapper.objectToJson(txnStatus);
		assertThat(getJsonString("$.igpTxnId",json),is("1"));
		assertThat(getJsonString("$.igpPlayId",json),is(MeshIgpPlayIdPresets.DEFAULT));
		assertThat(getJsonString("$.status",json),is("OK"));
		assertThat(getJsonString("$.wallet.type",json),is("ACCOUNT"));
		assertThat(getJsonString("$.token.token",json),is(MeshAuthorizationPresets.DEFAULT_TOKEN));
	}

	@Test
	public void nullIgpTxnIdNotIncludedOnSerialization() {
		final var txnStatus = new MeshGameTxnStatusBuilder()
				.withIgpTxnId(null)
				.get();
		verifyFieldsNotIncludedOnSerialization(txnStatus, "$.igpTxnId");
	}

	@Test
	public void nullIgpPlayIdNotIncludedOnSerialization() {
		final var txnStatus = new MeshGameTxnStatusBuilder()
				.withIgpPlayId(null)
				.get();
		verifyFieldsNotIncludedOnSerialization(txnStatus, "$.igpPlayId");
	}

	@Test
	public void nullWalletNotIncludedOnSerialization() {
		final var txnStatus = new MeshGameTxnStatusBuilder()
				.withWallet(null)
				.get();
		verifyFieldsNotIncludedOnSerialization(txnStatus, "$.wallet");
	}

	@Test
	public void nullTokenNotIncludedOnSerialization() {
		final var txnStatus = new MeshGameTxnStatusBuilder()
				.withToken(null)
				.get();
		verifyFieldsNotIncludedOnSerialization(txnStatus, "$.token");
	}

	@Test
	public void nullTxnTsNotIncludedOnSerialization() {
		final var txnStatus = new MeshGameTxnStatusBuilder()
				.withTxnTs(null)
				.get();
		verifyFieldsNotIncludedOnSerialization(txnStatus, "$.txnTs");
	}

	private void verifyFieldsNotIncludedOnSerialization(
			final MeshGameTxnStatus txnStatus, final String... paths) {
		final var json = jsonMapper.objectToJson(txnStatus);
		thrown.expect(PathNotFoundException.class);

		Arrays.asList(paths).forEach(path -> getJsonString(path, json));
	}

	@Test
	public void notNullFields() {
		MeshGameTxnStatus txnStatus = new MeshGameTxnStatusBuilder()
				.withStatus(null)
				.get();
		assertThat(numberOfValidationErrors(txnStatus),is(1));
	}

	@Test
	public void successfulDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnStatus-valid.json");
		MeshGameTxnStatus txnStatus = jsonMapper.jsonToObject(json, MeshGameTxnStatus.class);
		assertThat(txnStatus.getIgpTxnId(),is("txn-1"));
		assertThat(txnStatus.getIgpPlayId(),is("play-1"));
		assertThat(txnStatus.getStatus(),is(Status.OK));
		assertThat(txnStatus.getTxnTs(),is(equalTo(Date.from(ZonedDateTime.of(2020,1,1,9,0,0,0,ZoneId.of("UTC")).toInstant()))));
		assertThat(txnStatus.getWallet().getType(),is(MeshWalletType.ACCOUNT));
		assertThat(txnStatus.getToken().getToken(),is("abcdefghijkl"));
	}

	@Test
	public void successfulMinimalDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnStatus-minimal.json");
		MeshGameTxnStatus txnStatus = jsonMapper.jsonToObject(json, MeshGameTxnStatus.class);
		assertThat(txnStatus.getIgpTxnId(),is(nullValue()));
		assertThat(txnStatus.getStatus(),is(Status.OK));
	}

	@Test
	public void unknownFieldIgnoredOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnStatus-ignoreProperties.json");
		MeshGameTxnStatus txnStatus = jsonMapper.jsonToObject(json, MeshGameTxnStatus.class);
		assertThat(txnStatus.getStatus(),is(Status.OK));
	}
}
