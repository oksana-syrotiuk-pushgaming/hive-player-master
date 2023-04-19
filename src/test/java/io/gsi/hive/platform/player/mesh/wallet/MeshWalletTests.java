/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player.mesh.wallet;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.mesh.presets.MeshWalletPresets;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class MeshWalletTests extends DomainTestBase {

	@Test
	public void deserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/Wallet-valid.json");
		MeshWallet wallet = jsonMapper.jsonToObject(json, MeshWallet.class);
		assertRequiredFieldsOnDeserialization(wallet);
	}

	@Test
	public void deserializationWithOptionalFields() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/WalletWithOptionalFields-valid.json");
		MeshWallet wallet = jsonMapper.jsonToObject(json, MeshWallet.class);
		assertRequiredFieldsOnDeserialization(wallet);
		assertThat(wallet.getMessage(), equalTo(MeshWalletPresets.WALLET_MESSAGE));
	}

	private void assertRequiredFieldsOnDeserialization(final MeshWallet wallet) {
		assertThat(wallet.getType(), is(MeshWalletType.ACCOUNT));
		assertThat(wallet.getBalance(), is(new BigDecimal("110.00")));
		assertThat(wallet.getFunds().get(0).getType(), is(MeshWalletFundType.CASH));
		assertThat(((MeshWalletBalanceFund) wallet.getFunds().get(0)).getBalance(), is(new BigDecimal("100.00")));
		assertThat(wallet.getFunds().get(1).getType(), is(MeshWalletFundType.BONUS));
		assertThat(((MeshWalletBalanceFund) wallet.getFunds().get(1)).getBalance(), is(new BigDecimal("10.00")));
	}

	@Test
	public void serialization() {
		MeshWallet wallet = new MeshWalletBuilder().get();
		String json = jsonMapper.objectToJson(wallet);
		assertRequiredFieldsOnSerialization(json);
	}

	@Test
	public void serializationWithOptionalFields() {
		MeshWallet wallet = new MeshWalletBuilder()
				.withMessage(MeshWalletPresets.WALLET_MESSAGE)
				.get();
		String json = jsonMapper.objectToJson(wallet);
		assertRequiredFieldsOnSerialization(json);

		assertThat(getJsonString("$.message.content", json), is(MeshWalletPresets.WALLET_MESSAGE_CONTENT));
		assertThat(getJsonString("$.message.type", json), is(MeshWalletPresets.WALLET_MESSAGE_TYPE));
		assertThat(getJsonString("$.message.format", json), is(MeshWalletPresets.WALLET_MESSAGE_FORMAT));
	}

	private void assertRequiredFieldsOnSerialization(final String json) {
		assertThat(getJsonString("$.type", json), is("ACCOUNT"));
		assertThat(getJsonString("$.currency", json), is("GBP"));
		assertThat(getJsonNumber("$.balance", json), is(1000.00));
		assertThat(getJsonString("$.funds[0].type", json), is("CASH"));
		assertThat(getJsonString("$.funds[1].type", json), is("BONUS"));
		assertThat(getJsonNumber("$.funds[0].balance", json), is(995.00));
		assertThat(getJsonNumber("$.funds[1].balance", json), is(5.00));
		assertThat(getJsonNumber("$.funds[1].balance", json), is(5.00));
	}

	@Test
	public void serializationWithFunds() {
		MeshWallet wallet = new MeshWalletBuilder()
				.withFunds(
						MeshWalletFundPresets.getMeshWalletBalanceFund(),
						MeshWalletFundPresets.getMeshWalletBalanceFund(MeshWalletFundType.BONUS, new BigDecimal("10.00")),
						new MeshWalletOperatorFreeroundsFund()
				)
				.get();
		String json = jsonMapper.objectToJson(wallet);
		assertThat(getJsonString("$.type",json),is("ACCOUNT"));
		assertThat(getJsonString("$.currency",json),is("GBP"));
		assertThat(getJsonNumber("$.balance",json),is(1000.00));
		assertThat(getJsonString("$.funds[0].type",json),is("CASH"));
		assertThat(getJsonString("$.funds[1].type",json),is("BONUS"));
		assertThat(getJsonNumber("$.funds[0].balance",json),is(1000.00));
		assertThat(getJsonNumber("$.funds[1].balance",json),is(10.00));
	}

	@Test
	public void unknownFieldIgnoredOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/Wallet-ignoreProperties.json");
		MeshWallet wallet = jsonMapper.jsonToObject(json, MeshWallet.class);
		assertThat(wallet.getType(),is(MeshWalletType.ACCOUNT));
	}

	@Test
	public void notNullFields() {
		MeshWallet wallet = new MeshWalletBuilder().get();
		ReflectionTestUtils.setField(wallet, "type", null);
		ReflectionTestUtils.setField(wallet, "currency", null);
		ReflectionTestUtils.setField(wallet, "balance", null);
		ReflectionTestUtils.setField(wallet, "funds", null);
		assertThat(numberOfValidationErrors(wallet),is(4));
	}

	@Test
	public void invalidFieldOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/Wallet-invalidField.json");
		thrown.expect(IllegalArgumentException.class);
		jsonMapper.jsonToObject(json, MeshWallet.class);
	}

	@Test
	public void invalidMoneyFormatOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/Wallet-invalidMoneyFormat.json");
		MeshWallet wallet = jsonMapper.jsonToObject(json, MeshWallet.class);
		assertThat(numberOfValidationErrors(wallet),is(1));
	}

	@Test
	public void negativeBalanceOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/Wallet-negativeBalance.json");
		MeshWallet wallet = jsonMapper.jsonToObject(json, MeshWallet.class);
		assertThat(numberOfValidationErrors(wallet),is(1));
	}

	@Test
	public void invalidWalletTypeOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/Wallet-invalidWalletType.json");
		thrown.expect(IllegalArgumentException.class);
		jsonMapper.jsonToObject(json, MeshWallet.class);
	}

	@Test
	public void invalidFundTypeOnDeserialization() throws IOException {
		String json = getJsonMessage("classpath:json/mesh/wallet/Wallet-invalidFundType.json");
		thrown.expect(IllegalArgumentException.class);
		jsonMapper.jsonToObject(json, MeshWallet.class);
	}
}
