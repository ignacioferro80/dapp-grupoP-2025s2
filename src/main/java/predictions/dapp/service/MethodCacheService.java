package predictions.dapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.model.MethodAspects;
import predictions.dapp.repositories.MethodAspectsRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class MethodCacheService {

    private static final Logger logger = LoggerFactory.getLogger(MethodCacheService.class);
    private static final int CACHE_DURATION_MINUTES = 5;

    private final MethodAspectsRepository methodAspectsRepository;
    private final ObjectMapper objectMapper;

    // Statistics
    private long cacheHits = 0;
    private long cacheMisses = 0;

    public MethodCacheService(MethodAspectsRepository methodAspectsRepository) {
        this.methodAspectsRepository = methodAspectsRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Retrieve cached result if valid - Generic version for any Class type
     */
    public <T> Optional<T> getCachedResult(String methodSignature, Class<T> resultType) {
        try {
            Optional<MethodAspects> cached = methodAspectsRepository.findValidCache(
                    methodSignature,
                    LocalDateTime.now()
            );

            if (cached.isPresent()) {
                cacheHits++;
                T result = objectMapper.readValue(cached.get().getLastResult(), resultType);
                logger.debug("Cache HIT for: {}", methodSignature);
                return Optional.of(result);
            } else {
                cacheMisses++;
                logger.debug("Cache MISS for: {}", methodSignature);
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cached result for {}: {}", methodSignature, e.getMessage());
            cacheMisses++;
            return Optional.empty();
        }
    }

    /**
     * Retrieve cached result if valid - TypeReference version for complex generic types like Map<String, Object>
     */
    public <T> Optional<T> getCachedResult(String methodSignature, TypeReference<T> typeReference) {
        try {
            Optional<MethodAspects> cached = methodAspectsRepository.findValidCache(
                    methodSignature,
                    LocalDateTime.now()
            );

            if (cached.isPresent()) {
                cacheHits++;
                T result = objectMapper.readValue(cached.get().getLastResult(), typeReference);
                logger.debug("Cache HIT for: {}", methodSignature);
                return Optional.of(result);
            } else {
                cacheMisses++;
                logger.debug("Cache MISS for: {}", methodSignature);
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cached result for {}: {}", methodSignature, e.getMessage());
            cacheMisses++;
            return Optional.empty();
        }
    }

    /**
     * Retrieve cached Map result - Convenience method for Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getCachedMapResult(String methodSignature) {
        return getCachedResult(methodSignature, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Store result in cache
     */
    @Transactional
    public void cacheResult(String methodSignature, Object result) {
        try {
            String jsonResult = objectMapper.writeValueAsString(result);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(CACHE_DURATION_MINUTES);

            Optional<MethodAspects> existing = methodAspectsRepository.findByMethodSignature(methodSignature);

            if (existing.isPresent()) {
                // Update existing cache entry
                MethodAspects cache = existing.get();
                cache.setLastResult(jsonResult);
                cache.setCreatedAt(now);
                cache.setExpiresAt(expiresAt);
                methodAspectsRepository.save(cache);
                logger.debug("Updated cache for: {}", methodSignature);
            } else {
                // Create new cache entry
                MethodAspects cache = new MethodAspects(methodSignature, jsonResult, now, expiresAt);
                methodAspectsRepository.save(cache);
                logger.debug("Created cache for: {}", methodSignature);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing result for {}: {}", methodSignature, e.getMessage());
        }
    }

    /**
     * Scheduled cleanup - runs every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    @Transactional
    public void cleanupExpiredCache() {
        int deleted = methodAspectsRepository.deleteExpiredEntries(LocalDateTime.now());
        if (deleted > 0) {
            logger.info("Cleaned up {} expired cache entries", deleted);
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStatistics() {
        long validEntries = methodAspectsRepository.countValidEntries(LocalDateTime.now());
        return new CacheStats(cacheHits, cacheMisses, validEntries);
    }

    /**
     * Reset statistics (useful for testing)
     */
    public void resetStatistics() {
        cacheHits = 0;
        cacheMisses = 0;
    }

    /**
     * Cache statistics holder
     */
    public static class CacheStats {
        private final long hits;
        private final long misses;
        private final long validEntries;

        public CacheStats(long hits, long misses, long validEntries) {
            this.hits = hits;
            this.misses = misses;
            this.validEntries = validEntries;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public long getValidEntries() {
            return validEntries;
        }

        public double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total * 100 : 0.0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, validEntries=%d, hitRate=%.2f%%}",
                    hits, misses, validEntries, getHitRate());
        }
    }
}