package io.gsi.hive.platform.player.txn.search;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.hive.platform.player.cache.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TxnSearchService {

    private final Long maxDateRangeForSearchInDays;
    private final TxnSearchDao txnSearchDao;

    public TxnSearchService(TxnSearchDao txnSearchDao,  @Value("${hive.maxDaysForDateRangeInSearch:31}") String maxValue) {
        this.txnSearchDao = txnSearchDao;
        this.maxDateRangeForSearchInDays = Long.valueOf(maxValue);
    }

    @Cacheable(cacheNames = CacheConfig.TXN_SEARCH_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
    public Page<TxnSearchRecord> search(TxnSearchArguments txnSearchArguments) {
        try {
            List<TxnSearchRecord> txnSearchRecords = txnSearchDao.search(txnSearchArguments);
            long totalRecords = txnSearchRecords.size() < txnSearchArguments.getPageSize() ?
                txnSearchRecords.size() : txnSearchDao.recordCount(txnSearchArguments);
            Pageable pageData = PageRequest.of(txnSearchArguments.getPage(), txnSearchArguments.getPageSize());
            return new PageImpl<>(txnSearchRecords, pageData, totalRecords);
        } catch (Exception ignored) {
            throw new InternalServerException("Txn search failed.");
        }
    }

    public boolean isDateRangeWithinAcceptableBoundary(ZonedDateTime from, ZonedDateTime to) {
        Long differenceInDays = ChronoUnit.DAYS.between(from, to);
        return maxDateRangeForSearchInDays.compareTo(differenceInDays) > 0;
    }
}
