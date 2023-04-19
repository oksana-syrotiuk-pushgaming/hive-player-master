package io.gsi.hive.platform.player.play.report;

import io.gsi.commons.exception.InternalServerException;
import io.gsi.hive.platform.player.cache.CacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayReportService
{
    private final PlayReportDao playReportDao;

    public PlayReportService(PlayReportDao playReportDao) {
        this.playReportDao = playReportDao;
    }

    @Cacheable(cacheNames = CacheConfig.PLAY_REPORT_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
    public List<PlayReportRecord> generateReport(PlayReportArguments playReportArguments)
    {
        try {
            return playReportDao.reportQuery(playReportArguments);
        } catch (Exception ignored) {
            throw new InternalServerException("Play report failed.");
        }
    }
}
