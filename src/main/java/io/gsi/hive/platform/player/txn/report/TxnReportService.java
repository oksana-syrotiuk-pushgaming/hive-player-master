package io.gsi.hive.platform.player.txn.report;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.hive.platform.player.cache.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TxnReportService {

    private final TxnReportDao txnReportDao;

    public TxnReportService(TxnReportDao txnReportDao) {
        this.txnReportDao = txnReportDao;
    }

    @Cacheable(cacheNames = CacheConfig.TXN_REPORT_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
    public List<TxnReportRecord> generateReport(TxnReportArguments txnReportArguments) {
        try {
            return txnReportDao.reportQuery(txnReportArguments);
        } catch (Exception ignored) {
            throw new InternalServerException("Txn report failed.");
        }
    }
}
