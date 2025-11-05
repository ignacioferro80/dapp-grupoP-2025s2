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

@RestController
@RequestMapping("/api/football")
@Tag(name = "Football Data", description = "Public football data APIs - competitions, teams, matches, and fixtures from Football-Data.org")
public class FootballDataController {

    private final FootballDataService service;

    public FootballDataController(FootballDataService service) {
        this.service = service;
    }

    @GetMapping("/competitions")
    @Operation(
            summary = "Get all available competitions",
            description = "Retrieves a list of all football competitions available in the Football-Data API, including leagues from various countries"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved competitions list",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"competitions\": [\n" +
                                            "    {\n" +
                                            "      \"id\": 2021,\n" +
                                            "      \"name\": \"Premier League\",\n" +
                                            "      \"code\": \"PL\",\n" +
                                            "      \"area\": {\"name\": \"England\"}\n" +
                                            "    },\n" +
                                            "    {\n" +
                                            "      \"id\": 2014,\n" +
                                            "      \"name\": \"La Liga\",\n" +
                                            "      \"code\": \"PD\",\n" +
                                            "      \"area\": {\"name\": \"Spain\"}\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<JsonNode> competitions() throws Exception {
        return ResponseEntity.ok(service.getCompetitions());
    }

    @GetMapping("/competitions/{code}/matches")
    @Operation(
            summary = "Get matches by competition",
            description = "Retrieves all matches for a specific competition. Optionally filter by matchday number to get matches from a specific round"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved matches",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"matches\": [\n" +
                                            "    {\n" +
                                            "      \"id\": 123456,\n" +
                                            "      \"homeTeam\": {\"name\": \"Arsenal\"},\n" +
                                            "      \"awayTeam\": {\"name\": \"Chelsea\"},\n" +
                                            "      \"score\": {\"fullTime\": {\"home\": 2, \"away\": 1}},\n" +
                                            "      \"status\": \"FINISHED\"\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<JsonNode> matchesByCompetition(
            @Parameter(description = "Competition code (e.g., PL, PD, SA, BL1)", example = "PL", required = true)
            @PathVariable String code,
            @Parameter(description = "Optional matchday number to filter specific round", example = "28")
            @RequestParam(required = false) Integer matchday
    ) throws Exception {
        return ResponseEntity.ok(service.getMatchesByCompetition(code, matchday));
    }

    @GetMapping("/competitions/{code}/results")
    @Operation(
            summary = "Get finished match results by competition",
            description = "Retrieves only completed matches (FINISHED status) for a specific competition, showing final scores"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved finished matches",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class)
                    )
            )
    })
    public ResponseEntity<JsonNode> resultsByCompetition(
            @Parameter(description = "Competition code", example = "PL", required = true)
            @PathVariable String code) throws Exception {
        return ResponseEntity.ok(service.getResultsByCompetition(code));
    }

    @GetMapping("/competitions/{code}/fixtures")
    @Operation(
            summary = "Get upcoming fixtures by competition",
            description = "Retrieves only scheduled future matches (SCHEDULED status) for a specific competition"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved fixtures",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class)
                    )
            )
    })
    public ResponseEntity<JsonNode> fixturesByCompetition(
            @Parameter(description = "Competition code", example = "PL", required = true)
            @PathVariable String code) throws Exception {
        return ResponseEntity.ok(service.getFixtures(code));
    }

    @GetMapping("/teams")
    @Operation(
            summary = "Get all available teams",
            description = "Retrieves a list of all football teams available in the Football-Data API"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved teams list",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"teams\": [\n" +
                                            "    {\n" +
                                            "      \"id\": 86,\n" +
                                            "      \"name\": \"Real Madrid CF\",\n" +
                                            "      \"shortName\": \"Real Madrid\",\n" +
                                            "      \"founded\": 1902\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<JsonNode> teams() throws Exception {
        return ResponseEntity.ok(service.getTeams());
    }

    @GetMapping("/teams/{id}/results")
    @Operation(
            summary = "Get finished match results by team",
            description = "Retrieves all completed matches for a specific team, showing final scores and outcomes"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved team results",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class)
                    )
            )
    })
    public ResponseEntity<JsonNode> resultsByTeam(
            @Parameter(description = "Team ID from Football-Data API", example = "86", required = true)
            @PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.getResultsByTeam(id));
    }

    @GetMapping("/teams/{id}/fixtures")
    @Operation(
            summary = "Get upcoming fixtures by team",
            description = "Retrieves all scheduled future matches for a specific team"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved team fixtures",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class)
                    )
            )
    })
    public ResponseEntity<JsonNode> fixturesByTeam(
            @Parameter(description = "Team ID from Football-Data API", example = "86", required = true)
            @PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.getFixturesByTeam(id));
    }

    @GetMapping("/teams/{id}/lastResult")
    @Operation(
            summary = "Get last match result by team",
            description = "Retrieves the most recent completed match for a specific team"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved last match result",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"matches\": [\n" +
                                            "    {\n" +
                                            "      \"homeTeam\": {\"name\": \"Real Madrid\"},\n" +
                                            "      \"awayTeam\": {\"name\": \"Barcelona\"},\n" +
                                            "      \"score\": {\"fullTime\": {\"home\": 3, \"away\": 1}},\n" +
                                            "      \"utcDate\": \"2024-10-26T20:00:00Z\"\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<JsonNode> lastResultByTeam(
            @Parameter(description = "Team ID from Football-Data API", example = "86", required = true)
            @PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.getLastResultByTeam(id));
    }

    @GetMapping("/teams/{id}/futureMatches")
    @Operation(
            summary = "Get future matches for team until end of year",
            description = "Retrieves all scheduled matches for a specific team from today until December 31st of the current year"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved future matches",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"matches\": [\n" +
                                            "    {\n" +
                                            "      \"homeTeam\": {\"name\": \"Real Madrid\"},\n" +
                                            "      \"awayTeam\": {\"name\": \"Atletico Madrid\"},\n" +
                                            "      \"utcDate\": \"2024-11-15T20:00:00Z\",\n" +
                                            "      \"status\": \"SCHEDULED\"\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<JsonNode> getFutureMatchesByTeamFromNowToEndOfYear(
            @Parameter(description = "Team ID from Football-Data API", example = "86", required = true)
            @PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.getFutureMatchesByTeamFromNowToEndOfYear(id));
    }
}