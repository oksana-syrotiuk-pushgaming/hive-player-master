package io.gsi.hive.platform.player.presets;

import io.gsi.hive.platform.player.session.Mode;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.gsi.hive.platform.player.txn.TxnType;
import io.gsi.hive.platform.player.txn.search.TxnSearchRecord;
import io.gsi.hive.platform.player.txn.search.TxnSearchRecordBuilder;

import java.math.BigDecimal;

public interface SearchPresets
{
    static Mode MODE = Mode.real;
    static Boolean GUEST = Boolean.FALSE;
    static Boolean BONUS = false;
    static BigDecimal AMOUNT = new BigDecimal("10.00");
    static BigDecimal JACKPOT_AMOUNT = new BigDecimal("100.00");
    static TxnStatus STATUS = TxnStatus.OK;
    static Integer TOTAL_ELEMENTS = 3;
    static Boolean LAST = Boolean.TRUE;
    static Integer TOTAL_PAGES = 1;
    static Integer SIZE = 20;
    static Integer NUMBER = 0;
    static String SORT = null;
    static Integer NUMBER_OF_ELEMENTS = 3;
    static Boolean FIRST = true;
    static TxnSearchRecord DEFAULT_TUPLE = TxnSearchRecordBuilder
            .aTxnSearchRecord().build();
    static int PAGE = 0;
    static int PAGE_SIZE = 100;
    static TxnType TYPE = TxnType.STAKE;
    static String COUNTRY = "GB";
}
