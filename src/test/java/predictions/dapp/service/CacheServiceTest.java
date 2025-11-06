package predictions.dapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    private CacheService cacheService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService();
        mapper = new ObjectMapper();
    }

    @Test
    void cachePrediction_AndRetrieve_Success() {
        // Arrange
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("winner", "Team A");
        prediction.put("probability", "65.5%");

        // Act
        cacheService.cachePrediction("86", "65", prediction);
        Map<String, Object> retrieved = cacheService.getPrediction("86", "65");

        // Assert
        assertNotNull(retrieved);
        assertEquals("Team A", retrieved.get("winner"));
        assertEquals("65.5%", retrieved.get("probability"));
    }

    @Test
    void getPrediction_NotCached_ReturnsNull() {
        // Act
        Map<String, Object> result = cacheService.getPrediction("99", "100");

        // Assert
        assertNull(result);
    }

    @Test
    void cachePrediction_NormalizedKeys_SameResult() {
        // Arrange
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("winner", "Team A");

        // Act - cache with "86" and "65"
        cacheService.cachePrediction("86", "65", prediction);

        // Assert - retrieve with "65" and "86" (reversed order)
        Map<String, Object> retrieved = cacheService.getPrediction("65", "86");
        assertNotNull(retrieved);
        assertEquals("Team A", retrieved.get("winner"));
    }

    @Test
    void cachePerformance_AndRetrieve_Success() {
        // Arrange
        ObjectNode performance = mapper.createObjectNode();
        performance.put("playerId", 44);
        performance.put("name", "Harry Kane");
        performance.put("goals", 25);

        // Act
        cacheService.cachePerformance("44", performance);
        ObjectNode retrieved = cacheService.getPerformance("44");

        // Assert
        assertNotNull(retrieved);
        assertEquals(44, retrieved.get("playerId").asInt());
        assertEquals("Harry Kane", retrieved.get("name").asText());
        assertEquals(25, retrieved.get("goals").asInt());
    }

    @Test
    void getPerformance_NotCached_ReturnsNull() {
        // Act
        ObjectNode result = cacheService.getPerformance("999");

        // Assert
        assertNull(result);
    }

    @Test
    void clearAllCaches_RemovesAllEntries() {
        // Arrange
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("winner", "Team A");
        ObjectNode performance = mapper.createObjectNode();
        performance.put("playerId", 44);

        cacheService.cachePrediction("86", "65", prediction);
        cacheService.cachePerformance("44", performance);

        // Act
        cacheService.clearAllCaches();

        // Assert
        assertNull(cacheService.getPrediction("86", "65"));
        assertNull(cacheService.getPerformance("44"));
    }

    @Test
    void getStats_ReturnsCorrectCounts() {
        // Arrange
        Map<String, Object> pred1 = new HashMap<>();
        pred1.put("winner", "Team A");
        Map<String, Object> pred2 = new HashMap<>();
        pred2.put("winner", "Team B");

        ObjectNode perf1 = mapper.createObjectNode();
        perf1.put("playerId", 44);

        cacheService.cachePrediction("86", "65", pred1);
        cacheService.cachePrediction("90", "91", pred2);
        cacheService.cachePerformance("44", perf1);

        // Act
        CacheService.CacheStats stats = cacheService.getStats();

        // Assert
        assertEquals(2, stats.getTotalPredictions());
        assertEquals(1, stats.getTotalPerformance());
        assertEquals(2, stats.getActivePredictions());
        assertEquals(1, stats.getActivePerformance());
    }

    @Test
    void clearExpiredEntries_DoesNotRemoveActiveEntries() {
        // Arrange
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("winner", "Team A");

        cacheService.cachePrediction("86", "65", prediction);

        // Act
        cacheService.clearExpiredEntries();

        // Assert - entry should still be there since it's not expired
        Map<String, Object> retrieved = cacheService.getPrediction("86", "65");
        assertNotNull(retrieved);
    }
}