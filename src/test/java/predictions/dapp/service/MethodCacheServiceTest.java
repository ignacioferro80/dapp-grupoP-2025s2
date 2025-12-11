package predictions.dapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import predictions.dapp.model.MethodAspects;
import predictions.dapp.repositories.MethodAspectsRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MethodCacheServiceTest {

    @Mock
    private MethodAspectsRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MethodCacheService methodCacheService;

    private Map<String, Object> testResult;
    private String testSignature;
    private String testJson;

    @BeforeEach
    void setUp() {
        testSignature = "compareTeams:86:65";
        testResult = Map.of(
                "team1", Map.of("id", "86", "name", "Arsenal"),
                "team2", Map.of("id", "65", "name", "Manchester City")
        );
        testJson = "{\"team1\":{\"id\":\"86\",\"name\":\"Arsenal\"},\"team2\":{\"id\":\"65\",\"name\":\"Manchester City\"}}";
    }

    @Tag("unit")
    @Test
    void testGenerateSignature_SingleParam() {
        String signature = methodCacheService.generateSignature("testMethod", "param1");
        assertEquals("testMethod:param1", signature);
    }

    @Tag("unit")
    @Test
    void testGenerateSignature_MultipleParams() {
        String signature = methodCacheService.generateSignature("compareTeams", "86", "65");
        assertEquals("compareTeams:86:65", signature);
    }

    @Tag("unit")
    @Test
    void testGenerateSignature_NoParams() {
        String signature = methodCacheService.generateSignature("testMethod");
        assertEquals("testMethod", signature);
    }

    @Tag("unit")
    @Test
    void testGetCachedResult_CacheHit() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        MethodAspects cachedEntry = new MethodAspects(
                testSignature,
                testJson,
                now.minusMinutes(2),
                now.plusMinutes(3)
        );

        when(repository.findValidCachedMethod(eq(testSignature), any(LocalDateTime.class)))
                .thenReturn(Optional.of(cachedEntry));
        when(objectMapper.readValue(eq(testJson), eq(Map.class)))
                .thenReturn(testResult);

        Optional<Map<String, Object>> result = methodCacheService.getCachedResult(testSignature);

        assertTrue(result.isPresent());
        assertEquals(testResult, result.get());
        verify(repository).findValidCachedMethod(eq(testSignature), any(LocalDateTime.class));
        verify(objectMapper).readValue(testJson, Map.class);
    }

    @Tag("unit")
    @Test
    void testGetCachedResult_CacheMiss() {
        when(repository.findValidCachedMethod(eq(testSignature), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        Optional<Map<String, Object>> result = methodCacheService.getCachedResult(testSignature);

        assertFalse(result.isPresent());
        verify(repository).findValidCachedMethod(eq(testSignature), any(LocalDateTime.class));
        verify(objectMapper, never()).readValue(anyString(), eq(Map.class));
    }

    @Tag("unit")
    @Test
    void testGetCachedResult_DeserializationError() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        MethodAspects cachedEntry = new MethodAspects(
                testSignature,
                "invalid-json",
                now.minusMinutes(2),
                now.plusMinutes(3)
        );

        when(repository.findValidCachedMethod(eq(testSignature), any(LocalDateTime.class)))
                .thenReturn(Optional.of(cachedEntry));
        when(objectMapper.readValue(eq("invalid-json"), eq(Map.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {});

        Optional<Map<String, Object>> result = methodCacheService.getCachedResult(testSignature);

        // Should return empty on deserialization error instead of throwing
        assertFalse(result.isPresent());
    }

    @Tag("unit")
    @Test
    void testCacheResult_NewEntry() throws Exception {
        when(objectMapper.writeValueAsString(testResult)).thenReturn(testJson);
        when(repository.findByMethodSignature(testSignature)).thenReturn(Optional.empty());
        when(repository.save(any(MethodAspects.class))).thenAnswer(i -> i.getArgument(0));

        methodCacheService.cacheResult(testSignature, testResult);

        verify(objectMapper).writeValueAsString(testResult);
        verify(repository).findByMethodSignature(testSignature);
        verify(repository).save(argThat(entry ->
                entry.getMethodSignature().equals(testSignature) &&
                        entry.getLastResult().equals(testJson) &&
                        entry.getCreatedAt() != null &&
                        entry.getExpiresAt() != null
        ));
    }

    @Tag("unit")
    @Test
    void testCacheResult_UpdateExisting() throws Exception {
        LocalDateTime oldTime = LocalDateTime.now().minusMinutes(10);
        MethodAspects existingEntry = new MethodAspects(
                testSignature,
                "old-json",
                oldTime,
                oldTime.plusMinutes(5)
        );
        existingEntry.setId(1L);

        when(objectMapper.writeValueAsString(testResult)).thenReturn(testJson);
        when(repository.findByMethodSignature(testSignature)).thenReturn(Optional.of(existingEntry));
        when(repository.save(any(MethodAspects.class))).thenAnswer(i -> i.getArgument(0));

        methodCacheService.cacheResult(testSignature, testResult);

        verify(objectMapper).writeValueAsString(testResult);
        verify(repository).findByMethodSignature(testSignature);
        verify(repository).save(argThat(entry ->
                entry.getId().equals(1L) &&
                        entry.getMethodSignature().equals(testSignature) &&
                        entry.getLastResult().equals(testJson) &&
                        entry.getCreatedAt().isAfter(oldTime)
        ));
    }

    @Tag("unit")
    @Test
    void testCleanupExpiredEntries() {
        when(repository.deleteExpiredEntries(any(LocalDateTime.class))).thenReturn(5);

        methodCacheService.cleanupExpiredEntries();

        verify(repository).deleteExpiredEntries(any(LocalDateTime.class));
    }

    @Tag("unit")
    @Test
    void testCleanupExpiredEntries_NoExpired() {
        when(repository.deleteExpiredEntries(any(LocalDateTime.class))).thenReturn(0);

        methodCacheService.cleanupExpiredEntries();

        verify(repository).deleteExpiredEntries(any(LocalDateTime.class));
    }

    @Tag("unit")
    @Test
    void testClearAllCache() {
        when(repository.count()).thenReturn(10L);
        doNothing().when(repository).deleteAll();

        methodCacheService.clearAllCache();

        verify(repository).count();
        verify(repository).deleteAll();
    }

    @Tag("unit")
    @Test
    void testGetCacheStats() {
        when(repository.count()).thenReturn(15L);

        Map<String, Object> stats = methodCacheService.getCacheStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("totalEntries"));
        assertTrue(stats.containsKey("cacheDurationMinutes"));
        assertEquals(15L, stats.get("totalEntries"));
        assertEquals(5, stats.get("cacheDurationMinutes"));
    }
}