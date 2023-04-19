package io.gsi.hive.platform.player.play.search;

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
public class PlaySearchService
{
    private final Long maxDateRangeForSearchInDays;
    private final PlaySearchDao playSearchDao;

    public PlaySearchService(@Value("${hive.maxDaysForDateRangeInSearch:31}") String maxDateRangeForSearchInDays,
            PlaySearchDao playSearchDao) {
        this.maxDateRangeForSearchInDays = Long.valueOf(maxDateRangeForSearchInDays);
        this.playSearchDao = playSearchDao;
    }

    @Cacheable(cacheNames = CacheConfig.PLAY_SEARCH_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
    public Page<PlaySearchRecord> search(PlaySearchArguments playSearchArguments)
    {
        try {
            List<PlaySearchRecord> playSearchRecords = playSearchDao.search(playSearchArguments);
            Long totalRecords = playSearchDao.recordCount(playSearchArguments);
            Pageable pageData = PageRequest.of(playSearchArguments.getPage(), playSearchArguments.getPageSize());
            return new PageImpl<>(playSearchRecords,pageData, totalRecords);
        } catch (Exception ignored) {
            throw new InternalServerException("Play search failed.");
        }
    }

    public Boolean isDateRangeWithinAcceptableBoundary(ZonedDateTime from, ZonedDateTime to)
    {
        Long differenceInDays = ChronoUnit.DAYS.between(from, to);
        return maxDateRangeForSearchInDays.compareTo(differenceInDays) > 0;
    }
}
