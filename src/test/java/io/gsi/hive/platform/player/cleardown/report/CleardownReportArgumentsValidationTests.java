package io.gsi.hive.platform.player.cleardown.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;

public class CleardownReportArgumentsValidationTests extends DomainTestBase {

    @Test
    public void ok() {
        CleardownReportArguments cleardownReportArguments = CleardownReportArgumentsBuilders.defaultCleardownReportArgumentsBuilder().build();
        assertThat(numberOfValidationErrors(cleardownReportArguments), is(0));
    }

    @Test
    public void okWithOptionals() {
        CleardownReportArguments cleardownReportArguments = CleardownReportArgumentsBuilders.defaultCleardownReportArgumentsBuilder()
                .playerId(PlayerPresets.PLAYERID)
                .gameCode(GamePresets.CODE)
                .status(PlayStatus.ACTIVE)
                .ccyCode(WalletPresets.CURRENCY)
                .build();
        assertThat(numberOfValidationErrors(cleardownReportArguments), is(0));
    }

    @Test
    public void failMissingMandatory() {
        CleardownReportArguments cleardownReportArguments = CleardownReportArgumentsBuilders.defaultCleardownReportArgumentsBuilder()
                .dateFrom(null)
                .dateTo(null)
                .groupBy(Collections.emptySet())
                .build();
        assertThat(numberOfValidationErrors(cleardownReportArguments), is(3));
    }

    @Test
    public void failBadGroupBy() {
        CleardownReportArguments cleardownReportArguments = CleardownReportArgumentsBuilders.defaultCleardownReportArgumentsBuilder()
                .groupBy(new HashSet<>())
                .build();
        assertThat(numberOfValidationErrors(cleardownReportArguments), is(1));
    }
}
