package predictions.dapp.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import predictions.dapp.service.CacheService;

/**
 * Scheduled task to clean up expired cache entries periodically
 */
@Component
public class CacheMaintenanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CacheMaintenanceScheduler.class);

    private final CacheService cacheService;

    public CacheMaintenanceScheduler(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Clean expired cache entries every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void cleanExpiredCacheEntries() {
        logger.info("Starting scheduled cache cleanup");

        CacheService.CacheStats statsBefore = cacheService.getStats();
        logger.info("Cache stats before cleanup: {}", statsBefore);

        cacheService.clearExpiredEntries();

        CacheService.CacheStats statsAfter = cacheService.getStats();
        logger.info("Cache stats after cleanup: {}", statsAfter);
    }
}