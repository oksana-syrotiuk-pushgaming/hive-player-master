package io.gsi.hive.platform.player.mesh.wallet;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.bonus.wallet.OperatorFreeroundsFund;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.wallet.FundType;
import io.gsi.hive.platform.player.wallet.Message;
import io.gsi.hive.platform.player.wallet.Wallet;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WalletTests extends DomainTestBase {

    @Test
    public void deserializationWithFreeroundsFund() throws IOException {
        String json = getJsonMessage("classpath:json/mesh/wallet/WalletWithFreeroundsFund.json");
        Wallet wallet = jsonMapper.jsonToObject(json, Wallet.class);
        assertRequiredFieldsOnDeserializationFreeroundsFund(wallet);
    }

    @Test
    public void deserializationWithOperatorFreeroundsFund() throws IOException {
        String json = getJsonMessage("classpath:json/mesh/wallet/WalletWithOperatorFreeroundsFund.json");
        Wallet wallet = jsonMapper.jsonToObject(json, Wallet.class);
        assertRequiredFieldsOnDeserializationOperatorFreeroundsFund(wallet);
    }

    @Test
    public void serializationWithOperatorFreeroundsFund() {
        Wallet wallet = WalletPresets.walletWithOperatorFreeroundsFund();
        String json = jsonMapper.objectToJson(wallet);
        assertRequiredFieldsOnSerializationOperatorFreeroundsFund(json);
    }

    private void assertRequiredFieldsOnSerializationOperatorFreeroundsFund(final String json) {
        assertThat(getJsonNumber("$.balance", json), is(110.00));
        assertThat(getJsonString("$.funds[0].type", json), is("OPERATOR_FREEROUNDS"));
        assertThat(getJsonString("$.funds[0].awardId", json), is("award1"));
        assertThat(getJsonString("$.funds[0].bonusId", json), is("bonus1"));
        assertThat(getJsonObject("$.funds[0].extraInfo", json), is(WalletPresets.EXTRA_INFO));
        assertThat(getJsonString("$.message.content", json), is("content"));
        assertThat(getJsonString("$.message.format", json), is("format"));
        assertThat(getJsonString("$.message.type", json), is("type"));
    }

    private void assertRequiredFieldsOnDeserializationFreeroundsFund(final Wallet wallet) {
        assertThat(wallet.getBalance(), is(new BigDecimal("110.00")));
        assertThat(wallet.getFunds().get(0).getType(), is(FundType.FREEROUNDS));

        FreeroundsFund fund = (FreeroundsFund) wallet.getFunds().get(0);
        assertThat(fund.getAwarded(), is(10));
        assertThat(fund.getAwardId(), is(WalletPresets.AWARD_ID));
        assertThat(fund.getBonusId(), is(WalletPresets.BONUS_ID));
        assertThat(fund.getCumulativeWin(), is(new BigDecimal("0.00")));
        assertThat(fund.getFundId(), is(WalletPresets.BONUSFUNDID));
        assertThat(fund.getRemaining(), is(10));
        assertThat(fund.getBetAmount(), is(new BigDecimal("1.00")));

        assertMessage(wallet.getMessage());
    }

    private void assertRequiredFieldsOnDeserializationOperatorFreeroundsFund(final Wallet wallet) {
        assertThat(wallet.getBalance(), is(new BigDecimal("110.00")));
        assertThat(wallet.getFunds().get(0).getType(), is(FundType.OPERATOR_FREEROUNDS));

        OperatorFreeroundsFund fund = (OperatorFreeroundsFund) wallet.getFunds().get(0);
        assertThat(fund.getAwardId(), is(WalletPresets.AWARD_ID));
        assertThat(fund.getBonusId(), is(WalletPresets.BONUS_ID));
        assertThat(fund.getExtraInfo(), is(WalletPresets.EXTRA_INFO));

        assertMessage(wallet.getMessage());
    }

    private void assertMessage(Message message) {
        assertThat(message.getContent(), is("content"));
        assertThat(message.getType(), is("type"));
        assertThat(message.getFormat(), is("format"));
    }
}
