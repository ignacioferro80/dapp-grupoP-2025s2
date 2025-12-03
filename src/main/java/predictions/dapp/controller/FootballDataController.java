package predictions.dapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import predictions.dapp.service.FootballDataService;
import predictions.dapp.service.MetricsService;

@RestController
@RequestMapping("/api/football")
@Tag(name = "Football Data", description = "Public football data APIs - competitions, teams, matches, and fixtures from Football-Data.org")
public class FootballDataController {

    private final FootballDataService service;
    private final MetricsService metricsService;

    public FootballDataController(FootballDataService service, MetricsService metricsService) {
        this.service = service;
        this.metricsService = metricsService;
    }

    // ============================================================
    //  INTERNAL DUPLICATION REMOVAL (no new classes)
    // ============================================================
    @FunctionalInterface
    private interface JsonSupplier {
        JsonNode get() throws Exception;
    }

    private ResponseEntity<JsonNode> execute(JsonSupplier supplier) {
        metricsService.incrementRequests();
        return (ResponseEntity<JsonNode>) metricsService.measureLatency(() -> {
            try {
                return ResponseEntity.ok(supplier.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                metricsService.incrementErrors();
                return ResponseEntity.internalServerError().body(e.getMessage());
            } catch (Exception e) {
                metricsService.incrementErrors();
                return ResponseEntity.internalServerError().body(e.getMessage());
            }
        });
    }

    // ============================================================
    //  ENDPOINTS — NO FUNCTIONALITY CHANGED
    // ============================================================

    @GetMapping("/competitions")
    @Operation(
            summary = "Get all available competitions",
            description = "Retrieves a list of all football competitions available in the Football-Data API."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved competitions list",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "competitions": [
                                                {"id": 2021, "name": "Premier League"},
                                                {"id": 2014, "name": "La Liga"}
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<JsonNode> competitions() {
        return execute(service::getCompetitions);
    }

    @GetMapping("/competitions/{code}/matches")
    @Operation(summary = "Get matches by competition")
    public ResponseEntity<JsonNode> matchesByCompetition(
            @PathVariable String code,
            @RequestParam(required = false) Integer matchday
    ) {
        return execute(() -> service.getMatchesByCompetition(code, matchday));
    }

    @GetMapping("/competitions/{code}/results")
    @Operation(summary = "Get finished match results by competition")
    public ResponseEntity<JsonNode> resultsByCompetition(@PathVariable String code) {
        return execute(() -> service.getResultsByCompetition(code));
    }

    @GetMapping("/competitions/{code}/fixtures")
    @Operation(summary = "Get upcoming fixtures by competition")
    public ResponseEntity<JsonNode> fixturesByCompetition(@PathVariable String code) {
        return execute(() -> service.getFixtures(code));
    }

    @GetMapping("/teams")
    @Operation(summary = "Get all available teams")
    public ResponseEntity<JsonNode> teams() {
        return execute(service::getTeams);
    }

    @GetMapping("/teams/{id}/results")
    @Operation(summary = "Get finished match results by team")
    public ResponseEntity<JsonNode> resultsByTeam(@PathVariable String id) {
        return execute(() -> service.getResultsByTeam(id));
    }

    @GetMapping("/teams/{id}/fixtures")
    @Operation(summary = "Get upcoming fixtures by team")
    public ResponseEntity<JsonNode> fixturesByTeam(@PathVariable String id) {
        return execute(() -> service.getFixturesByTeam(id));
    }

    @GetMapping("/teams/{id}/lastResult")
    @Operation(summary = "Get last match result by team")
    public ResponseEntity<JsonNode> lastResultByTeam(@PathVariable String id) {
        return execute(() -> service.getLastResultByTeam(id));
    }

    @GetMapping("/teams/{id}/futureMatches")
    @Operation(summary = "Get future matches until end of year by team")
    public ResponseEntity<JsonNode> getFutureMatchesByTeamFromNowToEndOfYear(@PathVariable String id) {
        return execute(() -> service.getFutureMatchesByTeamFromNowToEndOfYear(id));
    }
}
