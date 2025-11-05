package predictions.dapp.scrapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhoScoredScraper {

    private static final Logger logger = LoggerFactory.getLogger(WhoScoredScraper.class);

    // HTTP Header Constants
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "en-US,en;q=0.9";
    private static final String ACCEPT_ENCODING = "gzip, deflate, br";
    private static final String CONNECTION = "keep-alive";
    private static final int TIMEOUT_MS = 5000;

    // Method to obtain player statistics
    public List<String> getPlayerStatistics(String playerUrl) {
        List<String> statistics = new ArrayList<>();
        try {
            Document doc = fetchDocument(playerUrl);

            // Select player statistics using specific CSS classes
            Elements playerData = doc.select("div.player-stats");
            for (org.jsoup.nodes.Element data : playerData) {
                statistics.add(data.text());
            }
        } catch (IOException e) {
            logger.error("Failed to fetch player statistics from {}", playerUrl, e);
            return Collections.emptyList();
        }
        return statistics;
    }

    // Method to obtain team ratings for a match
    public List<String> getTeamRatings(String matchUrl) {
        List<String> ratings = new ArrayList<>();
        try {
            Document doc = fetchDocument(matchUrl);

            // Select team ratings from the page using specific HTML elements
            Elements teamRatings = doc.select("div.team-ratings");
            for (org.jsoup.nodes.Element rating : teamRatings) {
                ratings.add(rating.text());
            }
        } catch (IOException e) {
            logger.error("Failed to fetch team ratings from {}", matchUrl, e);
            return Collections.emptyList();
        }
        return ratings;
    }

    // Method to obtain game patterns (passes, shots, possession)
    public List<String> getGamePatterns(String matchUrl) {
        List<String> patterns = new ArrayList<>();
        try {
            Document doc = fetchDocument(matchUrl);

            // Extract game patterns stats using specific CSS classes
            Elements gamePatterns = doc.select("div.game-statistics");
            for (org.jsoup.nodes.Element pattern : gamePatterns) {
                patterns.add(pattern.text());
            }
        } catch (IOException e) {
            logger.error("Failed to fetch game patterns from {}", matchUrl, e);
            return Collections.emptyList();
        }
        return patterns;
    }

    // Centralized method to fetch documents with consistent headers
    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", ACCEPT_HEADER)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept-Encoding", ACCEPT_ENCODING)
                .header("Connection", CONNECTION)
                .timeout(TIMEOUT_MS)
                .get();
    }
}