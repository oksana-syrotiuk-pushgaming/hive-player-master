package io.gsi.hive.platform.player.api.bo;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.txn.search.TxnSearchArguments;
import io.gsi.hive.platform.player.txn.search.TxnSearchRecord;
import io.gsi.hive.platform.player.txn.search.TxnSearchService;
import java.util.Collections;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bo/platform/player/v1")
@Loggable
public class TxnSearchController
{
    private final TxnSearchService txnSearchService;
    @Autowired
    public TxnSearchController(final TxnSearchService txnSearchService)
    {
        this.txnSearchService = txnSearchService;
    }

    @GetMapping(path="/txn/search")
    public Page<TxnSearchRecord> performTxnSearch(@ModelAttribute @Valid TxnSearchArguments txnSearchArguments)
    {
        if(txnSearchService.isDateRangeWithinAcceptableBoundary(
                txnSearchArguments.getDateFrom(), txnSearchArguments.getDateTo()))
        {
            return txnSearchService.search(txnSearchArguments);
        }
        else
        {
            throw new IllegalArgumentException("date range too large");
        }
    }

    /**
     * @deprecated
     *  Deprecated due to implementing multiple igp code txn search
     */
    @GetMapping(path="/igp/{igpCode}/txn/search")
    @Deprecated
    public Page<TxnSearchRecord> performTxnSearch(@PathVariable("igpCode") String igpCode,
        @ModelAttribute @Valid TxnSearchArguments txnSearchArguments)
    {
        txnSearchArguments.setIgpCodes(Collections.singletonList(igpCode));
        return this.performTxnSearch(txnSearchArguments);
    }
}
