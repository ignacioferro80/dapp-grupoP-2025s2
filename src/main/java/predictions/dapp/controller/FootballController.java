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

    /** GET /api/football/competitions */
    @GetMapping("/competitions")
    public ResponseEntity<JsonNode> competitions() throws Exception {
        return ResponseEntity.ok(service.getCompetitions());
    }

    /** GET /api/football/competitions/{code}/matches?matchday=28 */
    @GetMapping("/competitions/{code}/matches")
    public ResponseEntity<JsonNode> matchesByCompetition(
            @PathVariable String code,
            @RequestParam(required = false) Integer matchday
    ) throws Exception {
        return ResponseEntity.ok(service.getMatches(code, matchday));
    }

}
