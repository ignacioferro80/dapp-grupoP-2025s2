package predictions.dapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache service with TTL (Time To Live) support.
 * Reduces API calls by caching responses for 1 hour.
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static final long CACHE_TTL_MINUTES = 60; // 1 hour

    private final ObjectMapper mapper = new ObjectMapper();

    // Separate caches for different data types
    private final Map<String, CacheEntry> predictionCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> performanceCache = new ConcurrentHashMap<>();

    /**
     * Get prediction from cache if available and not expired
     *
     * @param team1Id First team ID
     * @param team2Id Second team ID
     * @return Cached prediction or null if not found/expired
     */
    public Map<String, Object> getPrediction(String team1Id, String team2Id) {
        String cacheKey = generatePredictionKey(team1Id, team2Id);
        return getCachedData(predictionCache, cacheKey, "Prediction");
    }

    /**
     * Cache a prediction result
     *
     * @param team1Id First team ID
     * @param team2Id Second team ID
     * @param prediction Prediction data to cache
     */
    public void cachePrediction(String team1Id, String team2Id, Map<String, Object> prediction) {
        String cacheKey = generatePredictionKey(team1Id, team2Id);
        cacheData(predictionCache, cacheKey, prediction, "Prediction");
    }

    /**
     * Get player performance from cache if available and not expired
     *
     * @param playerId Player ID
     * @return Cached performance data or null if not found/expired
     */
    public ObjectNode getPerformance(String playerId) {
        String cacheKey = generatePerformanceKey(playerId);
        Map<String, Object> cached = getCachedData(performanceCache, cacheKey, "Performance");

        if (cached == null) {
            return null;
        }

        // Convert Map back to ObjectNode
        try {
            String json = mapper.writeValueAsString(cached);
            return (ObjectNode) mapper.readTree(json);
        } catch (JsonProcessingException e) {
            logger.error("Error converting cached performance to ObjectNode", e);
            return null;
        }
    }

    /**
     * Cache player performance data
     *
     * @param playerId Player ID
     * @param performance Performance data to cache
     */
    public void cachePerformance(String playerId, ObjectNode performance) {
        String cacheKey = generatePerformanceKey(playerId);

        // Convert ObjectNode to Map for storage
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> performanceMap = mapper.convertValue(performance, Map.class);
            cacheData(performanceCache, cacheKey, performanceMap, "Performance");
        } catch (Exception e) {
            logger.error("Error caching performance data", e);
        }
    }

    /**
     * Clear expired entries from all caches (maintenance operation)
     */
    public void clearExpiredEntries() {
        int removedPredictions = clearExpiredFromCache(predictionCache, "Prediction");
        int removedPerformance = clearExpiredFromCache(performanceCache, "Performance");

        if (removedPredictions > 0 || removedPerformance > 0) {
            logger.info("Cleared {} expired prediction(s) and {} expired performance(s)",
                    removedPredictions, removedPerformance);
        }
    }

    /**
     * Clear all caches (useful for testing or manual cache invalidation)
     */
    public void clearAllCaches() {
        predictionCache.clear();
        performanceCache.clear();
        logger.info("All caches cleared");
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getStats() {
        return new CacheStats(
                predictionCache.size(),
                performanceCache.size(),
                countActiveEntries(predictionCache),
                countActiveEntries(performanceCache)
        );
    }

    // ==================== Private Helper Methods ====================

    private String generatePredictionKey(String team1Id, String team2Id) {
        // Normalize key: always put smaller ID first for consistency
        // This way "86-65" and "65-86" produce the same key
        int id1 = Integer.parseInt(team1Id);
        int id2 = Integer.parseInt(team2Id);

        if (id1 <= id2) {
            return "pred:" + team1Id + ":" + team2Id;
        } else {
            return "pred:" + team2Id + ":" + team1Id;
        }
    }

    private String generatePerformanceKey(String playerId) {
        return "perf:" + playerId;
    }

    private Map<String, Object> getCachedData(Map<String, CacheEntry> cache,
                                              String key,
                                              String dataType) {
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            logger.debug("{} cache MISS for key: {}", dataType, key);
            return null;
        }

        if (entry.isExpired()) {
            logger.debug("{} cache EXPIRED for key: {}", dataType, key);
            cache.remove(key);
            return null;
        }

        logger.info("{} cache HIT for key: {} (age: {} minutes)",
                dataType, key, entry.getAgeInMinutes());
        return entry.data;
    }

    private void cacheData(Map<String, CacheEntry> cache,
                           String key,
                           Map<String, Object> data,
                           String dataType) {
        CacheEntry entry = new CacheEntry(data);
        cache.put(key, entry);
        logger.info("{} cached for key: {}", dataType, key);
    }

    private int clearExpiredFromCache(Map<String, CacheEntry> cache, String dataType) {
        int removed = 0;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        return removed;
    }

    private int countActiveEntries(Map<String, CacheEntry> cache) {
        return (int) cache.values().stream()
                .filter(entry -> !entry.isExpired())
                .count();
    }

    // ==================== Inner Classes ====================

    /**
     * Cache entry with TTL support
     */
    private static class CacheEntry {
        private final Map<String, Object> data;
        private final Instant createdAt;
        private final Instant expiresAt;

        CacheEntry(Map<String, Object> data) {
            this.data = data;
            this.createdAt = Instant.now();
            this.expiresAt = createdAt.plus(CACHE_TTL_MINUTES, ChronoUnit.MINUTES);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        long getAgeInMinutes() {
            return ChronoUnit.MINUTES.between(createdAt, Instant.now());
        }
    }

    /**
     * Cache statistics for monitoring
     */
    public static class CacheStats {
        private final int totalPredictions;
        private final int totalPerformance;
        private final int activePredictions;
        private final int activePerformance;

        public CacheStats(int totalPredictions, int totalPerformance,
                          int activePredictions, int activePerformance) {
            this.totalPredictions = totalPredictions;
            this.totalPerformance = totalPerformance;
            this.activePredictions = activePredictions;
            this.activePerformance = activePerformance;
        }

        public int getTotalPredictions() { return totalPredictions; }
        public int getTotalPerformance() { return totalPerformance; }
        public int getActivePredictions() { return activePredictions; }
        public int getActivePerformance() { return activePerformance; }

        @Override
        public String toString() {
            return String.format(
                    "CacheStats{predictions=%d(%d active), performance=%d(%d active)}",
                    totalPredictions, activePredictions, totalPerformance, activePerformance
            );
        }
    }
}