package io.gsi.hive.platform.player.txn.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.SearchPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import org.junit.Test;

public class TxnSearchArgumentsValidationIT extends DomainTestBase
{
    @Test
    public void ok() {
        TxnSearchArguments txnSearchArguments = TxnSearchArgumentsBuilders
                .defaultTxnSearchArgumentBuilder().build();
        assertThat(numberOfValidationErrors(txnSearchArguments), is(0));
    }

    @Test
    public void okWithOptionals() {
        TxnSearchArguments txnSearchArguments = TxnSearchArgumentsBuilders
                .defaultTxnSearchArgumentBuilder()
                .playerId(PlayerPresets.PLAYERID)
                .username(PlayerPresets.USERNAME)
                .gameCode(GamePresets.CODE)
                .type(SearchPresets.TYPE)
                .status(SearchPresets.STATUS)
                .ccyCode(WalletPresets.CURRENCY)
                .country(SearchPresets.COUNTRY)
                .txnId(TxnPresets.TXNID)
                .build();
        assertThat(numberOfValidationErrors(txnSearchArguments), is(0));
    }

    @Test
    public void failMissingMandatory() {
        TxnSearchArguments txnSearchArguments = TxnSearchArgumentsBuilders
                .defaultTxnSearchArgumentBuilder()
                .dateFrom(null)
                .dateTo(null)
                .build();
        assertThat(numberOfValidationErrors(txnSearchArguments), is(2));
    }
}
