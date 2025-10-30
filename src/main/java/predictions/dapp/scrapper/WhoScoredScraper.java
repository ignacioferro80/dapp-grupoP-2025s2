package predictions.dapp.scrapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WhoScoredScraper {

    private static final String BASE_URL = "https://www.whoscored.com";

    // Method to obtain player statistics
    public List<String> getPlayerStatistics(String playerUrl) throws IOException {
        List<String> statistics = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(playerUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // Set the User-Agent
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")  // Added Accept-Encoding for compression
                    .header("Connection", "keep-alive")
                    .timeout(5000)  // 5 seconds timeout for the request
                    .get();

            // Select player statistics using specific CSS classes
            Elements playerData = doc.select("div.player-stats"); // Ensure you are using the correct CSS selector
            for (org.jsoup.nodes.Element data : playerData) {
                statistics.add(data.text());
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch player statistics from " + playerUrl);
            throw e;
        }
        return statistics;
    }

    // Method to obtain team ratings for a match
    public List<String> getTeamRatings(String matchUrl) throws IOException {
        List<String> ratings = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(matchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .timeout(5000)
                    .get();

            // Select team ratings from the page using specific HTML elements
            Elements teamRatings = doc.select("div.team-ratings"); // Ensure you are using the correct CSS selector
            for (org.jsoup.nodes.Element rating : teamRatings) {
                ratings.add(rating.text());
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch team ratings from " + matchUrl);
            throw e;
        }
        return ratings;
    }

    // Method to obtain game patterns (passes, shots, possession)
    public List<String> getGamePatterns(String matchUrl) throws IOException {
        List<String> patterns = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(matchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .timeout(5000)
                    .get();

            // Extract game patterns stats using specific CSS classes
            Elements gamePatterns = doc.select("div.game-statistics"); // Ensure you are using the correct CSS selector
            for (org.jsoup.nodes.Element pattern : gamePatterns) {
                patterns.add(pattern.text());
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch game patterns from " + matchUrl);
            throw e;
        }
        return patterns;
    }
}
