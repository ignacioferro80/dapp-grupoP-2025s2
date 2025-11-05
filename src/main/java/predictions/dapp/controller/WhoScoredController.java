package predictions.dapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import predictions.dapp.scrapper.WhoScoredScraper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.List;

@RestController
@Tag(name = "WhoScored Scraper", description = "Web scraping APIs for player statistics and match data from WhoScored.com")
public class WhoScoredController {

    private WhoScoredScraper scraper = new WhoScoredScraper();

    @GetMapping("/api/player/{playerId}")
    @Operation(
            summary = "Get player statistics from WhoScored",
            description = "Scrapes detailed player statistics from WhoScored.com including performance metrics, ratings, and match data. " +
                    "Note: This endpoint relies on web scraping and may fail if WhoScored's website structure changes or implements rate limiting."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully scraped player statistics",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(
                                    value = "[\"Player: Cristiano Ronaldo\", \"Rating: 7.85\", \"Goals: 24\", \"Assists: 3\"]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to scrape player data - website unreachable or structure changed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "null")
                    )
            )
    })
    public List<String> getPlayerStatistics(
            @Parameter(description = "Player ID from WhoScored.com URL", example = "5583", required = true)
            @PathVariable String playerId) {
        try {
            String playerUrl = "https://www.whoscored.com/players/" + playerId;
            return scraper.getPlayerStatistics(playerUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/api/match/{matchId}/ratings")
    @Operation(
            summary = "Get team ratings for a match from WhoScored",
            description = "Scrapes team performance ratings for a specific match from WhoScored.com. " +
                    "Returns rating data for both teams including overall team performance scores. " +
                    "Note: This endpoint relies on web scraping and may be affected by website changes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully scraped team ratings",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(
                                    value = "[\"Home Team Rating: 7.2\", \"Away Team Rating: 6.8\", \"Match Performance: High Intensity\"]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to scrape team ratings - website unreachable or structure changed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "null")
                    )
            )
    })
    public List<String> getTeamRatings(
            @Parameter(description = "Match ID from WhoScored.com URL", example = "1234567", required = true)
            @PathVariable String matchId) {
        try {
            String matchUrl = "https://www.whoscored.com/Matches/" + matchId;
            return scraper.getTeamRatings(matchUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/api/match/{matchId}/patterns")
    @Operation(
            summary = "Get game patterns and statistics from WhoScored",
            description = "Scrapes detailed match statistics from WhoScored.com including passes completed, shots on target, " +
                    "possession percentages, and other tactical patterns. Provides insights into how the match was played. " +
                    "Note: This endpoint relies on web scraping and may be affected by website changes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully scraped game patterns",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(
                                    value = "[\"Possession: Home 58% - Away 42%\", \"Passes: Home 524 - Away 387\", \"Shots: Home 15 - Away 8\", \"Shots on Target: Home 6 - Away 3\"]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to scrape game patterns - website unreachable or structure changed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "null")
                    )
            )
    })
    public List<String> getGamePatterns(
            @Parameter(description = "Match ID from WhoScored.com URL", example = "1234567", required = true)
            @PathVariable String matchId) {
        try {
            String matchUrl = "https://www.whoscored.com/Matches/" + matchId;
            return scraper.getGamePatterns(matchUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}