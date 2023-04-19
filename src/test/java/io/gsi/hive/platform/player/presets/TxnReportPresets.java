package io.gsi.hive.platform.player.presets;

import static io.gsi.hive.platform.player.txn.report.TxnGroupBy.ccy_code;
import static io.gsi.hive.platform.player.txn.report.TxnGroupBy.game_code;

import io.gsi.hive.platform.player.txn.Mode;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.report.TxnGroupBy;
import io.gsi.hive.platform.player.txn.report.TxnOrderBy;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public interface TxnReportPresets
{
    static Long AGGREGATED_PLAYS = 500L;
    static Long AGGREGATED_UNIQUE_PLAYERS = 200L;
    static BigDecimal TOTAL_STAKE = new BigDecimal("2000.00");
    static BigDecimal TOTAL_WIN = new BigDecimal("1000.00");
    static BigDecimal BONUS_COST = new BigDecimal("345.00");
    static Long NUMBER_OF_FREEROUNDS = 234L;
    static Mode MODE = Mode.real;
    static Boolean GUEST = Boolean.FALSE;
    static Boolean BONUS = Boolean.FALSE;
    static String TYPE = "STAKE";
    static String STATUS = TxnStatus.OK.toString();
    static String COUNTRY = "GB";
    @SuppressWarnings("serial")
	static Set<TxnGroupBy> GROUP_BY = new HashSet<TxnGroupBy>(){
        {this.add(game_code);this.add(ccy_code);}
    };

    @SuppressWarnings("serial")
	static Set<TxnOrderBy> ORDER_BY = new HashSet<>();
}
