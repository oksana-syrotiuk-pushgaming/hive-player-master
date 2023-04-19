/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.txn;

import io.gsi.hive.platform.player.mesh.presets.MeshRgsTxnIdPresets;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import io.gsi.hive.platform.player.DomainTestBase;

import java.io.IOException;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MeshGameTxnCancelTests extends DomainTestBase
{

	@Test
	public void successfulSerialization() {

		MeshGameTxnCancel txnCancel = MeshGameTxnCancel.builder()
				.playerId(MeshRgsTxnIdPresets.DEFAULT)
				.rgsTxnCancelId("cancel-1")
				.playComplete(true)
				.roundComplete(true)
				.reason("VOID_GAME")
				.amount(new BigDecimal(20000000123.45))
				.rgsGameId("1011")
				.currency("GBP")
				.rgsPlayId("rgsPlayId1")
				.rgsRoundId("rgsRoundId1")
				.extraInfo(Map.of("info", "someinfo"))
				.build();

		String json = jsonMapper.objectToJson(txnCancel);

		assertThat(getJsonString("$.rgsTxnCancelId",json),is("cancel-1"));
		assertThat(getJsonBoolean("$.playComplete",json),is(true));
		assertThat(getJsonBoolean("$.roundComplete",json),is(true));
		assertThat(getJsonString("$.reason",json),is("VOID_GAME"));
		assertThat(getJsonString("$.currency", json), is("GBP"));
		assertThat(getJsonBigDecimal("$.amount", json), is(new BigDecimal(20000000123.45)));
		assertThat(getJsonString("$.rgsPlayId", json), is("rgsPlayId1"));
		assertThat(getJsonString("$.rgsRoundId", json), is("rgsRoundId1"));
		assertThat(getJsonString("$.rgsGameId", json), is("1011"));
		assertThat(getJsonString("$.extraInfo.info",json), is("someinfo"));
	}

	@Test
	public void successfulSerializationJsonMapperJsonToObject(){
		MeshGameTxnCancel txnCancel = MeshGameTxnCancel.builder()
				.playerId(MeshRgsTxnIdPresets.DEFAULT)
				.rgsTxnCancelId("cancel-1")
				.playComplete(true)
				.roundComplete(true)
				.reason("VOID_GAME")
				.amount(new BigDecimal(20000000123.45))
				.rgsGameId("1011")
				.currency("GBP")
				.rgsPlayId("rgsPlayId1")
				.rgsRoundId("rgsRoundId1")
				.extraInfo(Map.of("info", "someinfo"))
				.build();

		String json = jsonMapper.objectToJson(txnCancel);

		MeshGameTxnCancel actualMeshGameTxnCancel = jsonMapper.jsonToObject(json, MeshGameTxnCancel.class);
		Assertions.assertThat(txnCancel).isEqualToComparingFieldByField(actualMeshGameTxnCancel);
	}

	@Test
	public void successfulDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnCancel-valid.json");
		MeshGameTxnCancel txnCancel = jsonMapper.jsonToObject(json, MeshGameTxnCancel.class);
		assertThat(txnCancel.getRgsTxnCancelId(),is("cancel-1"));
		assertThat(txnCancel.getPlayComplete(),is(true));
		assertThat(txnCancel.getReason(),is("VOID_GAME"));
		assertThat(txnCancel.getCurrency(), is("GBP"));
		assertThat(txnCancel.getAmount(), is(new BigDecimal(20.00).setScale(2)));
		assertThat(txnCancel.getRgsPlayId(), is("playId1"));
		assertThat(txnCancel.getRgsRoundId(), is("round1"));
		assertThat(txnCancel.getRgsGameId(), is("1011"));
		assertThat(txnCancel.getExtraInfo(),is(Map.of("info", "someInfo")));
	}

	@Test
	public void missingPlayerId() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnCancel-missingPlayerId.json");
		MeshGameTxnCancel txnCancel = jsonMapper.jsonToObject(json, MeshGameTxnCancel.class);
		assertThat(numberOfValidationErrors(txnCancel),is(1));
	}

	@Test
	public void missingPlayComplete() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnCancel-missingPlayComplete.json");
		MeshGameTxnCancel txnCancel = jsonMapper.jsonToObject(json, MeshGameTxnCancel.class);
		assertThat(numberOfValidationErrors(txnCancel),is(1));
	}

	@Test
	public void missingCurrency() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnCancel-missingCurrency.json");
		MeshGameTxnCancel txnCancel = jsonMapper.jsonToObject(json, MeshGameTxnCancel.class);
		assertThat(numberOfValidationErrors(txnCancel),is(1));
	}

	@Test
	public void missingAmount() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnCancel-missingAmount.json");
		MeshGameTxnCancel txnCancel = jsonMapper.jsonToObject(json, MeshGameTxnCancel.class);
		assertThat(numberOfValidationErrors(txnCancel),is(1));
	}

}
