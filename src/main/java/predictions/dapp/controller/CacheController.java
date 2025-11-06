package predictions.dapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import predictions.dapp.service.CacheService;

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache Management", description = "Cache monitoring and management APIs")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get cache statistics",
            description = "Returns current cache statistics including total entries and active (non-expired) entries for predictions and performance data"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved cache statistics",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CacheService.CacheStats.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "totalPredictions": 15,
                                              "totalPerformance": 8,
                                              "activePredictions": 12,
                                              "activePerformance": 7
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<CacheService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(cacheService.getStats());
    }

    @DeleteMapping("/clear")
    @Operation(
            summary = "Clear all caches",
            description = "Manually clears all cached data (predictions and performance). Use with caution - this will force all subsequent requests to fetch fresh data from the API."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "All caches successfully cleared",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "All caches cleared successfully"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        cacheService.clearAllCaches();
        return ResponseEntity.ok(Map.of("message", "All caches cleared successfully"));
    }

    @DeleteMapping("/clear-expired")
    @Operation(
            summary = "Clear expired cache entries",
            description = "Manually triggers cleanup of expired cache entries. This is automatically done every 30 minutes, but can be triggered manually if needed."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Expired entries successfully cleared",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "Expired cache entries cleared"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, String>> clearExpiredEntries() {
        cacheService.clearExpiredEntries();
        return ResponseEntity.ok(Map.of("message", "Expired cache entries cleared"));
    }
}