/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.txn;

import io.gsi.hive.platform.player.DomainTestBase;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static io.gsi.hive.platform.player.presets.TimePresets.EXPECTED_STAKE_TXN_DEADLINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MeshGameTxnTests extends DomainTestBase {

	@Test
	public void successfulSerialization() {
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withActions(
					new MeshGameTxnActionBuilder().get(),
					new MeshGameTxnActionBuilder()
						.withType(MeshGameTxnActionType.WIN)
						.withAmount(new BigDecimal("100.00"))
						.get()
				).withTxnDeadline(ZonedDateTime.now().plusSeconds(30))
				.get();

		String json = jsonMapper.objectToJson(txn);
		assertThat(getJsonString("$.rgsTxnId",json),is("abcd-efgh-ijkl-mnop"));
		assertThat(getJsonString("$.actions[0].type",json),is("STAKE"));
		assertThat(getJsonString("$.actions[1].type",json),is("WIN"));
		assertThat(getJsonNumber("$.actions[0].amount",json),is(20.00));
		assertThat(getJsonNumber("$.actions[1].amount",json),is(100.00));
		assertThat(getJsonObject("$.txnDeadline",json),notNullValue());
	}

	@Test
	public void successfulDeserializationStake() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxn-stake-valid.json");
		MeshGameTxn txn = jsonMapper.jsonToObject(json, MeshGameTxn.class);
		assertThat(txn.getRgsTxnId(), is("rgs-txn-1"));
		assertThat(txn.getPlayerId(), is("player-1"));
		assertThat(txn.getPlayComplete(), is(false));
		assertThat(txn.getActions().size(), is(1));

		MeshGameTxnAction meshTxnAction = txn.getActions().get(0);
		assertThat(meshTxnAction.getType(), is(MeshGameTxnActionType.STAKE));
		assertThat(meshTxnAction.getAmount(), is(new BigDecimal("10.00")));
		assertThat(txn.getTxnDeadline(), equalTo(EXPECTED_STAKE_TXN_DEADLINE));
	}

	@Test
	public void successfulDeserializationWin() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxn-win-valid.json");
		MeshGameTxn txn = jsonMapper.jsonToObject(json, MeshGameTxn.class);
		assertThat(txn.getRgsTxnId(),is("rgs-txn-1"));
		assertThat(txn.getPlayerId(),is("player-1"));
		assertThat(txn.getPlayComplete(), is(true));
		assertThat(txn.getActions().size(),is(1));

		MeshGameTxnAction meshTxnAction = txn.getActions().get(0);
		assertThat(meshTxnAction.getType(),is(MeshGameTxnActionType.WIN));
		assertThat(meshTxnAction.getAmount(),is(new BigDecimal("100.00")));
	}

	@Test
	public void successfulSerializationWithFund() {
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withActions(
					new MeshGameTxnActionBuilder()
					.withType(MeshGameTxnActionType.RGS_FREEROUND_WIN)
					.withRgsActionId("10FreeSpins")
					.withAmount(new BigDecimal("10.00"))
					.get(),
					new MeshGameTxnActionBuilder()
						.withType(MeshGameTxnActionType.WIN)
						.withAmount(new BigDecimal("100.00"))
						.get()
				)
				.get();
		String json = jsonMapper.objectToJson(txn);
		assertThat(getJsonString("$.rgsTxnId",json),is("abcd-efgh-ijkl-mnop"));
		assertThat(getJsonString("$.actions[0].type",json),is("RGS_FREEROUND_WIN"));
		assertThat(getJsonString("$.actions[1].type",json),is("WIN"));
		assertThat(getJsonNumber("$.actions[0].amount",json),is(10.00));
		assertThat(getJsonNumber("$.actions[1].amount",json),is(100.00));
		assertThat(getJsonString("$.actions[0].rgsActionId",json),is("10FreeSpins"));
	}

	@Test
	public void successfulDeserializationWithFund() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxnWithFundAction-valid.json");
		MeshGameTxn txn = jsonMapper.jsonToObject(json, MeshGameTxn.class);
		assertThat(txn.getRgsTxnId(),is("rgs-txn-1"));
		assertThat(txn.getPlayerId(),is("player-1"));
		assertThat(txn.getPlayComplete(), is(true));
		assertThat(txn.getActions().size(),is(2));

		MeshGameTxnAction meshTxnFreeroundWinAction = txn.getActions().get(0);
		assertThat(meshTxnFreeroundWinAction.getType(),is(MeshGameTxnActionType.RGS_FREEROUND_WIN));
		assertThat(meshTxnFreeroundWinAction.getAmount(),is(new BigDecimal("10.00")));
		MeshGameTxnAction meshTxnWinAction = txn.getActions().get(1);
		assertThat(meshTxnWinAction.getType(),is(MeshGameTxnActionType.WIN));
		assertThat(meshTxnWinAction.getAmount(),is(new BigDecimal("100.00")));
	}

	@Test
	public void unknownFieldIgnoredOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxn-ignoreProperties.json");
		MeshGameTxn txn = jsonMapper.jsonToObject(json, MeshGameTxn.class);
		assertThat(txn.getRgsTxnId(),is("rgs-txn-1"));
	}

	@Test
	public void notNullFields() {
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withRgsTxnId(null)
				.withRgsGameId(null)
				.withRgsPlayId(null)
				.withRgsRoundId(null)
				.get();
		ReflectionTestUtils.setField(txn, "rgsTxnId", null);
		ReflectionTestUtils.setField(txn, "rgsGameId", null);
		ReflectionTestUtils.setField(txn, "rgsPlayId", null);
		ReflectionTestUtils.setField(txn, "rgsRoundId", null); // can be null
		assertThat(numberOfValidationErrors(txn),is(3));
	}

	@Test
	public void invalidCurrency() {
		MeshGameTxn txn = new MeshGameTxnBuilder().withCurrency("TOOLONG").get();
		assertThat(numberOfValidationErrors(txn),is(1));
		txn = new MeshGameTxnBuilder().withCurrency("T").get();
		assertThat(numberOfValidationErrors(txn),is(1));
	}

	@Test
	public void invalidMoneyFormat() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxn-invalidMoneyFormat.json");
		MeshGameTxn txn = jsonMapper.jsonToObject(json, MeshGameTxn.class);
		assertThat(numberOfValidationErrors(txn), is(1));
	}

	@Test
	public void actionWithNegativeAmount() {
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withActions(
					new MeshGameTxnActionBuilder().withAmount(new BigDecimal("-10.00")).get()
				)
				.get();
		assertThat(numberOfValidationErrors(txn),is(1));
	}

	@Test
	public void invalidActionType() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/txn/GameTxn-invalidActionType.json");
		thrown.expect(IllegalArgumentException.class);
		jsonMapper.jsonToObject(json, MeshGameTxn.class);
	}

	@Test
	public void emptyTransactionNotSupported() {
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withActions()
				.withPlayComplete(true)
				.withRoundComplete(true)
				.get();
		assertThat(numberOfValidationErrors(txn),is(1));
	}

	@Test
	public void emptyTransactionMustHavePlayCompleteFlagSetToTrue() {
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withPlayComplete(false)
				.withActions()
				.get();
		assertThat(numberOfValidationErrors(txn),is(1));
	}

	@Test
	public void multipleActionsInvalid() {
		MeshGameTxnAction meshGameStake1TxnAction = new MeshGameTxnActionBuilder()
				.withRgsActionId("stake-1")
				.get();
		MeshGameTxnAction meshGameStake2TxnAction = new MeshGameTxnActionBuilder()
				.withRgsActionId("stake-1")
				.get();
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withActions(meshGameStake1TxnAction, meshGameStake2TxnAction)
				.get();
		assertThat(numberOfValidationErrors(txn),is(1));
	}

	@Test
	public void mandatoryValuesNotNullOnStake() {
		MeshGameTxnAction meshGameStakeTxnAction = new MeshGameTxnActionBuilder()
				.withRgsActionId("stake-1")
				.withType(MeshGameTxnActionType.STAKE)
				.get();
		mandatoryValuesNotNullOnStake(meshGameStakeTxnAction);
	}

	private void mandatoryValuesNotNullOnStake(final MeshGameTxnAction meshTxnAction) {
		MeshGameTxn txn = new MeshGameTxnBuilder()
				.withActions(meshTxnAction)
				.withRgsGameId(null)
				.withPlayComplete(null)
				.withRoundComplete(null)
				.withRgsPlayId(null)
				.get();
		assertThat(numberOfValidationErrors(txn),is(3));
	}
}
