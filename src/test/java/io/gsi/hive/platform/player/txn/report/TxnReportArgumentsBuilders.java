package io.gsi.hive.platform.player.txn.report;

import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.PlayerPresets;
import io.gsi.hive.platform.player.presets.TimePresets;
import io.gsi.hive.platform.player.presets.TxnReportPresets;
import io.gsi.hive.platform.player.presets.WalletPresets;

public final class TxnReportArgumentsBuilders
{
    public static TxnReportArguments.TxnReportArgumentsBuilder defaultTxnReportArgumentsBuilder()
    {
        return TxnReportArguments.builder()
            .playerId(PlayerPresets.PLAYERID)
            .username(PlayerPresets.USERNAME)
            .gameCode(GamePresets.CODE)
            .mode(TxnReportPresets.MODE)
            .guest(TxnReportPresets.GUEST)
            .bonus(TxnReportPresets.BONUS)
            .type(TxnReportPresets.TYPE)
            .status(TxnReportPresets.STATUS)
            .ccyCode(WalletPresets.CURRENCY)
            .country(TxnReportPresets.COUNTRY)
            .groupBy(TxnReportPresets.GROUP_BY)
            .orderBy(TxnReportPresets.ORDER_BY)
            .dateFrom(TimePresets.ZONEDEPOCHUTC)
            .dateTo(TimePresets.ZONEDEPOCHUTC);
    }
}
