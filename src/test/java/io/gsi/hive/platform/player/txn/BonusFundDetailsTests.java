package io.gsi.hive.platform.player.txn;

import static org.assertj.core.api.Assertions.assertThat;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.txn.event.BonusFundDetails;
import io.gsi.hive.platform.player.txn.event.HiveBonusFundDetails;
import io.gsi.hive.platform.player.txn.event.OperatorBonusFundDetails;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

public class BonusFundDetailsTests extends DomainTestBase {

    @Test
    public void givenBonusDetailsJson_whenJsonToObject_shouldCreateObject() throws IOException {
        final var bonusDetailsJson = getJson("hiveBonusFundDetails-valid.json");
        final var bonusFundDetails = jsonMapper.jsonToObject(bonusDetailsJson, BonusFundDetails.class);

        assertBonusFundDetails(bonusFundDetails);
    }

    @Test
    public void givenBonusDetailsJsonWithoutType_whenToObject_shouldCreateObjectAndSetDefaultType() throws IOException {
        final var bonusDetailsJson = getJson("hiveBonusFundDetails-withoutType-valid.json");
        final var bonusDetails = jsonMapper.jsonToObject(bonusDetailsJson, BonusFundDetails.class);

        assertBonusFundDetails(bonusDetails);
    }

    @Test
    public void givenOperatorBonusDetailsJson_whenToObject_shouldCreateObject() throws IOException {
        final var operatorBonusDetailsJson = getJson("operatorBonusFundDetails-valid.json");
        final var bonusDetails = jsonMapper.jsonToObject(operatorBonusDetailsJson, BonusFundDetails.class);

        assertThat(bonusDetails).isNotNull();
        assertThat(bonusDetails.getType()).isEqualTo(OperatorBonusFundDetails.TYPE);

        final var operatorBonusFundDetails = (OperatorBonusFundDetails) bonusDetails;
        assertThat(operatorBonusFundDetails.getBonusId()).isEqualTo("bonus-1");
        assertThat(operatorBonusFundDetails.getAwardId()).isEqualTo("award-1");
        assertThat(operatorBonusFundDetails.getRemainingSpins()).isEqualTo(5);
        assertThat(operatorBonusFundDetails.getExtraInfo()).isEqualTo(Map.of("info", "info"));
    }

    private static void assertBonusFundDetails(final BonusFundDetails bonusFundDetails) {
        assertThat(bonusFundDetails).isNotNull();
        assertThat(bonusFundDetails.getType()).isEqualTo(HiveBonusFundDetails.TYPE);

        final var hiveBonusFundDetails = (HiveBonusFundDetails) bonusFundDetails;
        assertThat(hiveBonusFundDetails.getFundId()).isEqualTo(9308L);
    }

    private String getJson(final String fileName) throws IOException {
        return getJsonMessage("classpath:json/hive/txn/bonusFundDetails/" + fileName);
    }
}
