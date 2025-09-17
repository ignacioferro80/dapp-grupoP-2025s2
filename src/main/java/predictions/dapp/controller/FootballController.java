package predictions.dapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import predictions.dapp.service.FootballDataService;

@RestController
@RequestMapping("/api/football")
public class FootballController {

    private final FootballDataService service;

    public FootballController(FootballDataService service) {
        this.service = service;
    }

    /**
     * GET /api/football/competitions
     */
    @GetMapping("/competitions")
    public ResponseEntity<JsonNode> competitions() throws Exception {
        return ResponseEntity.ok(service.getCompetitions());
    }

    /**
     * GET /api/football/competitions/{code}/matches?matchday=28
     */
    @GetMapping("/competitions/{code}/matches")
    public ResponseEntity<JsonNode> matchesByCompetition(
            @PathVariable String code,
            @RequestParam(required = false) Integer matchday
    ) throws Exception {
        return ResponseEntity.ok(service.getMatchesByCompetition(code, matchday));
    }

    /**
     * GET /api/football/competitions/{code}/results
     */
    @GetMapping("/competitions/{code}/results")
    public ResponseEntity<JsonNode> resultsByCompetition(@PathVariable String code) throws Exception {
        return ResponseEntity.ok(service.getResultsByCompetition(code));
    }

    /**
     * GET /api/football/competitions/{code}/fixtures
     */
    @GetMapping("/competitions/{code}/fixtures")
    public ResponseEntity<JsonNode> fixturesByCompetition(@PathVariable String code) throws Exception {
        return ResponseEntity.ok(service.getFixtures(code));
    }

    @GetMapping("/teams")
    public ResponseEntity<JsonNode> teams() throws Exception {
        return ResponseEntity.ok(service.getTeams());
    }

    /** GET /api/football/teams/{id}/results */
    @GetMapping("/teams/{id}/results")
    public ResponseEntity<JsonNode> resultsByTeam(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.getResultsByTeam(id));
    }

    /** GET /api/football/teams/{id}/fixtures */
    @GetMapping("/teams/{id}/fixtures")
    public ResponseEntity<JsonNode> fixturesByTeam(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.getFixturesByTeam(id));
    }

    @GetMapping("/teams/{id}/lastResult")
    public ResponseEntity<JsonNode> lastResultByTeam(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(service.getLastResultByTeam(id));
    }

}
