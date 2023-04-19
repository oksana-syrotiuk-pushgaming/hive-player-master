package io.gsi.hive.platform.player.api.bo;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.txn.report.TxnReportArguments;
import io.gsi.hive.platform.player.txn.report.TxnReportRecord;
import io.gsi.hive.platform.player.txn.report.TxnReportService;
import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bo/platform/player/v1")
@Loggable
public class TxnReportController
{
    private final TxnReportService txnReportService;

    public TxnReportController(TxnReportService txnReportService) {
        this.txnReportService = txnReportService;
    }

    @GetMapping(path="/txn/report")
    public List<TxnReportRecord> performTxnReport(@ModelAttribute @Valid TxnReportArguments txnReportArguments)
    {
        return txnReportService.generateReport(txnReportArguments);
    }

    /**
     * @deprecated
     *  Deprecated due to implementing multiple igp code txn reporting
     */
    @GetMapping(path="/igp/{igpCode}/txn/report")
    @Deprecated
    public List<TxnReportRecord> performBackwardsCompatibleTxnReport(@PathVariable("igpCode") String igpCode,
        @ModelAttribute @Valid TxnReportArguments txnReportArguments)
    {
        txnReportArguments.setIgpCodes(Collections.singletonList(igpCode));
        return txnReportService.generateReport(txnReportArguments);
    }
}
