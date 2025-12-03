package predictions.dapp.cache;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import predictions.dapp.service.CacheService;

/**
 * Scheduled task to clean up expired cache entries periodically
 */
@Component
public class CacheMaintenanceScheduler {

    private final CacheService cacheService;

    public CacheMaintenanceScheduler(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Clean expired cache entries every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void cleanExpiredCacheEntries() {
        CacheService.CacheStats statsBefore = cacheService.getStats();

        cacheService.clearExpiredEntries();

        CacheService.CacheStats statsAfter = cacheService.getStats();
    }
}