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
import predictions.dapp.security.JwtUtil;
import predictions.dapp.service.MetricsService;
import predictions.dapp.service.PredictionService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Predictions", description = "Football match prediction APIs based on team statistics and performance")
public class PredictionController {

    private final PredictionService predictionService;
    private final MetricsService metricsService;
    private final JwtUtil jwtUtil;

    public PredictionController(PredictionService predictionService, MetricsService metricsService,
                                JwtUtil jwtUtil) {
        this.predictionService = predictionService;
        this.metricsService = metricsService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/predictions/{teamId1}/{teamId2}")
    @Operation(
            summary = "Predict match winner between two teams",
            description = "Analyzes team statistics including recent wins, goals, league standings, and goal differences to predict the winner. Calculates probability percentages for each team based on performance metrics. Requires authentication to save predictions to user history."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Prediction successfully generated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = "{\"probabilidad_Arsenal\": \"65.42%\", \"probabilidad_Chelsea\": \"34.58%\", \"prediction\": \"Arsenal con 65.42%\"}"
                            )
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
                    description = "Error processing prediction",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Error al realizar predicción\", \"details\": \"Team not found\"}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> predictMatchWinner(
            @Parameter(description = "ID of the first team from Football-Data API", example = "86", required = true)
            @PathVariable String teamId1,
            @Parameter(description = "ID of the second team from Football-Data API", example = "65", required = true)
            @PathVariable String teamId2) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("message", "User not logged in"));
        }

        try {
            String email = auth.getName();
            Long userId = jwtUtil.extractUserId(email);
            metricsService.incrementRequests();

            return metricsService.measureLatency(() -> {
                try {
                    Map<String, Object> prediction = predictionService.predictWinner(teamId1, teamId2, userId);
                    return ResponseEntity.ok(prediction);
                } catch (Exception e) {
                    metricsService.incrementErrors();
                    return ResponseEntity.internalServerError().body(e.getMessage());
                }
            });

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al realizar predicción",
                    "details", e.getMessage()
            ));
        }
    }
}