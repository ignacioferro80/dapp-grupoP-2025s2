package predictions.dapp.controller;

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
import org.springframework.web.bind.annotation.*;
import predictions.dapp.service.ComparisonService;
import predictions.dapp.service.MetricsService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Comparisons", description = "Football team comparison APIs based on team statistics and performance")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final MetricsService metricsService;

    public ComparisonController(ComparisonService comparisonService, MetricsService metricsService) {
        this.comparisonService = comparisonService;
        this.metricsService = metricsService;
    }

    @GetMapping("/compare/{teamId1}/{teamId2}")
    @Operation(
            summary = "Compare statistics between two teams",
            description = "Retrieves and compares detailed statistics for two teams including goals, matches played, performance metrics, and competition data. Uses caching to improve performance and reduce API calls."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Comparison successfully generated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"team1\": {\"id\": \"86\", \"name\": \"Arsenal FC\", \"wonGames\": 7, \"totalGoals\": 45, \"totalPoints\": 28, \"avgPosition\": 2.0, \"goalDifference\": 15, \"competitions\": [\"Premier League\", \"UEFA Champions League\"]}, \"team2\": {\"id\": \"65\", \"name\": \"Manchester City FC\", \"wonGames\": 8, \"totalGoals\": 52, \"totalPoints\": 32, \"avgPosition\": 1.0, \"goalDifference\": 20, \"competitions\": [\"Premier League\", \"UEFA Champions League\"]}}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error processing comparison",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Error al realizar comparación\", \"details\": \"Team not found\"}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> compareTeams(
            @Parameter(description = "ID of the first team from Football-Data API", example = "86", required = true)
            @PathVariable String teamId1,
            @Parameter(description = "ID of the second team from Football-Data API", example = "65", required = true)
            @PathVariable String teamId2) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("message", "User not logged in"));
        }

        try {
            metricsService.incrementRequests();

            return metricsService.measureLatency(() -> {
                try {
                    Map<String, Object> comparison = comparisonService.compareTeams(teamId1, teamId2);
                    return ResponseEntity.ok(comparison);
                } catch (Exception e) {
                    metricsService.incrementErrors();
                    return ResponseEntity.internalServerError().body(e.getMessage());
                }
            });

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al realizar comparación",
                    "details", e.getMessage()
            ));
        }
    }
}
