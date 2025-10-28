package predictions.dapp.controller;

import predictions.dapp.scrapper.WhoScoredScraper;  // Import the scraper from the 'scrapper' package
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.List;

@RestController
public class WhoScoredController {

    private WhoScoredScraper scraper = new WhoScoredScraper();

    // Endpoint to get player statistics
    @GetMapping("/api/player/{playerId}")
    public List<String> getPlayerStatistics(@PathVariable String playerId) {
        try {
            String playerUrl = "https://www.whoscored.com/players/" + playerId;
            return scraper.getPlayerStatistics(playerUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Endpoint to get team ratings for a match
    @GetMapping("/api/match/{matchId}/ratings")
    public List<String> getTeamRatings(@PathVariable String matchId) {
        try {
            String matchUrl = "https://www.whoscored.com/Matches/" + matchId;
            return scraper.getTeamRatings(matchUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Endpoint to get game patterns (passes, shots, possession)
    @GetMapping("/api/match/{matchId}/patterns")
    public List<String> getGamePatterns(@PathVariable String matchId) {
        try {
            String matchUrl = "https://www.whoscored.com/Matches/" + matchId;
            return scraper.getGamePatterns(matchUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
