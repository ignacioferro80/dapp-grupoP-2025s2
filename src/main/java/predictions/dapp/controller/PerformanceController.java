package predictions.dapp.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.MetricsService;
import predictions.dapp.service.PerformanceService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Performance", description = "Player performance analysis APIs - retrieves player statistics across competitions")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final JwtUtil jwtUtil;
    private final MetricsService metricsService;

    public PerformanceController(PerformanceService performanceService, JwtUtil jwtUtil, MetricsService metricsService) {
        this.performanceService = performanceService;
        this.jwtUtil = jwtUtil;
        this.metricsService = metricsService;
    }

    @GetMapping("/performance/{playerId}")
    @Operation(
            summary = "Get player performance statistics",
            description = "Retrieves comprehensive performance data for a specific player by searching across all major football competitions. " +
                    "Returns goals scored, matches played, performance ratio (goals per match), team, and competition. " +
                    "If player is not in top scorers, returns basic player information. Requires authentication to save to user history."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Player performance data successfully retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ObjectNode.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Top Scorer Found",
                                            value = """
                                                    {
                                                      "id": 44,
                                                      "name": "Harry Kane",
                                                      "team": "FC Bayern München",
                                                      "goals": 12,
                                                      "matches": 10,
                                                      "performance": 1.2,
                                                      "competition": "Bundesliga"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Player Not In Top Scorers",
                                            value = """
                                                    {
                                                      "id": 12345,
                                                      "name": "John Smith",
                                                      "team": "Example FC",
                                                      "performance": "Player 12345 John Smith performance is below average top players"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"User not logged in\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error fetching performance data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Failed to fetch performance data: Player not found\"}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> performance(
            @Parameter(description = "Player ID from Football-Data API", example = "44", required = true)
            @PathVariable String playerId) {
        metricsService.incrementRequests();
        return metricsService.measureLatency(() -> {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    String email = auth.getName();
                    Long userId = jwtUtil.extractUserId(email);

                    try {
                        ObjectNode response = performanceService.handlePerformance(userId, playerId);
                        return ResponseEntity.ok(response);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return ResponseEntity.status(500)
                                .body(Map.of("error", "Request interrupted: " + e.getMessage()));
                    } catch (Exception e) {
                        return ResponseEntity.status(500)
                                .body(Map.of("error", "Failed to fetch performance data: " + e.getMessage()));
                    }
                }
                return ResponseEntity.ok(Map.of("message", "User not logged in"));
            } catch (Exception e) {
                metricsService.incrementErrors();
                return ResponseEntity.internalServerError().body(e.getMessage());
            }
        });
    }
}