package io.gsi.hive.platform.player.txn.report;

import static io.gsi.hive.platform.player.txn.report.TxnGroupBy.country;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import io.gsi.hive.platform.player.DomainTestBase;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.TxnReportPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;
import io.gsi.hive.platform.player.txn.search.TxnSearchArguments;
import io.gsi.hive.platform.player.txn.search.TxnSearchArgumentsBuilders;

public class TxnReportArgumentsValidationIT extends DomainTestBase
{
    @Test
    public void ok() {
        TxnReportArguments txnSearchArguments = TxnReportArgumentsBuilders.defaultTxnReportArgumentsBuilder().build();
        assertThat(numberOfValidationErrors(txnSearchArguments), is(0));
    }

    @Test
    public void okWithOptionals() {
        TxnReportArguments txnReportArguments = TxnReportArgumentsBuilders
                .defaultTxnReportArgumentsBuilder()
                .playerId(PlayerPresets.PLAYERID)
                .username(PlayerPresets.USERNAME)
                .gameCode(GamePresets.CODE)
                .type(TxnReportPresets.TYPE)
                .status(TxnReportPresets.STATUS)
                .ccyCode(WalletPresets.CURRENCY)
                .country(TxnReportPresets.COUNTRY)
                .build();
        assertThat(numberOfValidationErrors(txnReportArguments), is(0));
    }

    @Test
    public void missingGroupBy() {
        TxnReportArguments txnReportArguments = TxnReportArgumentsBuilders
                .defaultTxnReportArgumentsBuilder()
                .groupBy(new HashSet<>())
                .build();
        assertThat(numberOfValidationErrors(txnReportArguments), is(1));
    }

    @Test
    public void groupByNotContainingCcyCode() {
        TxnReportArguments txnReportArguments = TxnReportArgumentsBuilders
                .defaultTxnReportArgumentsBuilder()
                .groupBy(new HashSet<>(Arrays.asList(country)))
                .build();
        assertThat(numberOfValidationErrors(txnReportArguments), is(1));
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
