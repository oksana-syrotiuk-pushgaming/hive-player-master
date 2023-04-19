package io.gsi.hive.platform.player.play.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.mesh.presets.MeshRgsPlayIdPresets;
import io.gsi.hive.platform.player.play.PlayStatus;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import org.junit.Test;

public class PlaySearchArgumentsValidationIT extends DomainTestBase {

    @Test
    public void ok() {
        PlaySearchArguments playSearchArguments = PlaySearchArgumentsBuilders.defaultPlaySearchArgumentsBuilder().build();
        assertThat(numberOfValidationErrors(playSearchArguments), is(0));
    }

    @Test
    public void okWithOptionals() {
        PlaySearchArguments playSearchArguments = PlaySearchArgumentsBuilders.defaultPlaySearchArgumentsBuilder()
                .playerId(PlayerPresets.PLAYERID)
                .gameCode(GamePresets.CODE)
                .status(PlayStatus.ACTIVE)
                .ccyCode(WalletPresets.CURRENCY)
                .playerId(MeshRgsPlayIdPresets.DEFAULT)
                .build();
        assertThat(numberOfValidationErrors(playSearchArguments), is(0));
    }

    @Test
    public void failMissingMandatory() {
        PlaySearchArguments playSearchArguments = PlaySearchArgumentsBuilders.defaultPlaySearchArgumentsBuilder()
                .dateFrom(null)
                .dateTo(null)
                .build();
        assertThat(numberOfValidationErrors(playSearchArguments), is(2));
    }
}
