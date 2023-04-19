package io.gsi.hive.platform.player.play.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;

import java.util.HashSet;
import org.junit.Test;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;

public class PlayReportArgumentsValidationTests extends DomainTestBase {

    @Test
    public void ok() {
        PlayReportArguments playReportArguments = PlayReportArgumentsBuilders.defaultPlayReportArgumentsBuilder().build();
        assertThat(numberOfValidationErrors(playReportArguments), is(0));
    }

    @Test
    public void okWithOptionals() {
        PlayReportArguments playReportArguments = PlayReportArgumentsBuilders.defaultPlayReportArgumentsBuilder()
                .playerId(PlayerPresets.PLAYERID)
                .gameCode(GamePresets.CODE)
                .status(PlayStatus.ACTIVE)
                .ccyCode(WalletPresets.CURRENCY)
                .build();
        assertThat(numberOfValidationErrors(playReportArguments), is(0));
    }

    @Test
    public void failMissingMandatory() {
        PlayReportArguments playReportArguments = PlayReportArgumentsBuilders.defaultPlayReportArgumentsBuilder()
                .dateFrom(null)
                .dateTo(null)
                .groupBy(Collections.emptySet())
                .build();
        assertThat(numberOfValidationErrors(playReportArguments), is(3));
    }
    
    @Test
    public void failBadGroupBy() {
        PlayReportArguments playReportArguments = PlayReportArgumentsBuilders.defaultPlayReportArgumentsBuilder()
                .groupBy(new HashSet<>())
                .build();
        assertThat(numberOfValidationErrors(playReportArguments), is(1));
    }
}
