package io.gsi.hive.platform.player.cleardown.report;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.txn.search.TxnSearchRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CleardownReportService {

  private final CleardownReportDao cleardownReportDao;

  public CleardownReportService(CleardownReportDao cleardownReportDao) {
    this.cleardownReportDao = cleardownReportDao;
  }

  @Cacheable(cacheNames = CacheConfig.CLEARDOWN_REPORT_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
  public List<CleardownReportRecord> generateReport(
      CleardownReportArguments cleardownReportArguments) {
    try {
      return cleardownReportDao.reportQuery(cleardownReportArguments);
    } catch (Exception ignored) {
      throw new InternalServerException("Cleardown report failed.");
    }
  }
}
