package io.gsi.hive.platform.player.txn.search;

import io.gsi.hive.platform.player.presets.SearchPresets;
import io.gsi.hive.platform.player.presets.TimePresets;

public final class TxnSearchArgumentsBuilders
{

    public static TxnSearchArguments.TxnSearchArgumentsBuilder defaultTxnSearchArgumentBuilder()
    {
        return TxnSearchArguments.builder()
            .page(SearchPresets.PAGE)
            .pageSize(SearchPresets.PAGE_SIZE)
            .dateFrom(TimePresets.ZONEDEPOCHUTC)
            .dateTo(TimePresets.ZONEDEPOCHUTC);
    }
}
