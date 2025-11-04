package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceService.class);
    private final ConsultasRepository consultasRepository;
    private final FootballDataService footballDataService;
    private final ObjectMapper mapper = new ObjectMapper();

    // Delay between API calls in milliseconds (6 seconds = 10 requests per minute max)
    private static final long API_CALL_DELAY_MS = 6000;

    // Major football leagues to check first
    private static final List<String> PRIORITY_COMPETITION_IDS = List.of("2019", "2021", "2014", "2015", "2002");

    public PerformanceService(ConsultasRepository consultasRepository,
                              FootballDataService footballDataService) {
        this.consultasRepository = consultasRepository;
        this.footballDataService = footballDataService;
    }

    /**
     * Main entry point: Fetches performance data for a player
     * Strategy:
     * 1. Get all available competitions from API
     * 2. Search for player in major leagues first (faster)
     * 3. If not found, search in all other competitions
     * 4. If still not found, get basic player info as fallback
     * 5. Save result to database and return
     */
    @Transactional
    public ObjectNode handlePerformance(Long userId, String playerId) throws IOException, InterruptedException {
        logger.info("Fetching performance data for userId: {} and playerId: {}", userId, playerId);

        // STEP 1: Fetch all available competitions from API
        JsonNode allCompetitions = fetchAllCompetitions();

        // STEP 2: Organize competitions by priority (major leagues first)
        CompetitionLists organizedCompetitions = organizeCompetitionsByPriority(allCompetitions);

        // STEP 3: Search for player in competitions (priority first, then others)
        ObjectNode playerStats = searchForPlayerInAllCompetitions(
                organizedCompetitions.priorityCompetitions,
                organizedCompetitions.otherCompetitions,
                playerId
        );

        // STEP 4: If player found in top scorers, save and return
        if (playerStats != null) {
            savePlayerPerformanceToDatabase(userId, playerStats);
            return playerStats;
        }

        // STEP 5: Player not in any top scorers list - get basic info as fallback
        ObjectNode basicPlayerInfo = fetchBasicPlayerInfoAsFallback(playerId);
        savePlayerPerformanceToDatabase(userId, basicPlayerInfo);
        return basicPlayerInfo;
    }



    //Submethods helpers to code main method handlePerformance - detailed comments inside
    //Easier to read/understand the main method this way - Joaco

    /**
     * Fetches all football competitions from the API
     * Calls: getCompetitions() from FootballDataService
     * Returns: JsonNode with all competitions
     */
    private JsonNode fetchAllCompetitions() throws IOException, InterruptedException {
        JsonNode competitionsResponse = footballDataService.getCompetitions();
        JsonNode competitions = competitionsResponse.path("competitions");

        // Wait to respect API rate limit
        Thread.sleep(API_CALL_DELAY_MS);

        return competitions;
    }

    /**
     * Separates competitions into two lists: priority (major leagues) and others

     * Priority competitions (checked first for efficiency):
     * - 2019: Serie A (Italy)
     * - 2021: Premier League (England)
     * - 2014: La Liga (Spain)
     * - 2015: Ligue 1 (France)
     * - 2002: Bundesliga (Germany)

     * Returns: CompetitionLists object with both lists
     */
    private CompetitionLists organizeCompetitionsByPriority(JsonNode competitions) {
        List<JsonNode> priorityComps = new ArrayList<>();
        List<JsonNode> otherComps = new ArrayList<>();

        if (competitions.isArray()) {
            for (JsonNode competition : competitions) {
                String competitionId = competition.path("id").asText();

                if (PRIORITY_COMPETITION_IDS.contains(competitionId)) {
                    priorityComps.add(competition);
                } else {
                    otherComps.add(competition);
                }
            }
        }

        return new CompetitionLists(priorityComps, otherComps);
    }

    /**
     * Searches for player across all competitions
     * First searches priority competitions (major leagues) for efficiency.
     * If not found, searches all other competitions.
     * Calls: searchInCompetitions() (private method in this class)
     * Returns: ObjectNode with player stats if found, null if not found
     */
    private ObjectNode searchForPlayerInAllCompetitions(
            List<JsonNode> priorityCompetitions,
            List<JsonNode> otherCompetitions,
            String playerId) throws InterruptedException {

        // Try major leagues first (most players are here)
        ObjectNode playerStats = searchInCompetitions(priorityCompetitions, playerId);

        // If not found in major leagues, try all others
        if (playerStats == null) {
            playerStats = searchInCompetitions(otherCompetitions, playerId);
        }

        return playerStats;
    }

    /**
     * Searches for a player within a list of competitions
     *
     * For each competition:
     * 1. Gets top 200 scorers from that competition
     * 2. Searches through scorers for the player ID
     * 3. If found, returns player stats with competition name
     * 4. If not found, continues to next competition
     *
     * Calls:
     * - getTopScorersByCompetitionId() from FootballDataService
     * - findPlayerInTopScorers() (private method in this class)
     *
     * Returns: ObjectNode with player stats if found, null if not found in any
     */
    private ObjectNode searchInCompetitions(List<JsonNode> competitions, String playerId) throws InterruptedException {
        for (JsonNode competition : competitions) {
            String competitionId = competition.path("id").asText();
            String competitionName = competition.path("name").asText();

            logger.info("Checking competition: {} (ID: {})", competitionName, competitionId);

            try {
                // Get top 200 scorers for this competition in 2024 season
                JsonNode topScorers = footballDataService.getTopScorersByCompetitionId(competitionId, 200, "2024");

                // Wait after API call to respect rate limit
                Thread.sleep(API_CALL_DELAY_MS);

                // Search for the player in this competition's top scorers
                ObjectNode playerStats = findPlayerInTopScorers(topScorers, playerId);

                if (playerStats != null) {
                    // Player found! Add competition name and return
                    logger.info("Player {} found in competition: {}", playerId, competitionName);
                    playerStats.put("competition", competitionName);
                    return playerStats;
                }
            } catch (IOException e) {
                // Log error and continue to next competition
                logger.warn("Failed to get scorers for competition {}: {}", competitionName, e.getMessage());

                // If rate limit error (HTTP 429), wait longer before continuing
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    logger.warn("Rate limit hit, waiting 30 seconds...");
                    Thread.sleep(30000);
                }
                continue;
            }
        }

        return null; // Player not found in any of these competitions
    }

    /**
     * Searches for a specific player within a competition's top scorers list
     *
     * Loops through all scorers and compares player IDs.
     * When found, extracts and calculates:
     * - Player ID, name, team
     * - Goals scored
     * - Matches played
     * - Performance score (goals per match)
     *
     * Returns: ObjectNode with player stats if found, null if not found
     */
    private ObjectNode findPlayerInTopScorers(JsonNode topScorers, String playerId) {
        JsonNode scorers = topScorers.path("scorers");

        if (scorers.isArray()) {
            for (JsonNode scorer : scorers) {
                JsonNode player = scorer.path("player");
                int id = player.path("id").asInt(-1);

                // Check if this is the player we're looking for
                if (id == Integer.parseInt(playerId)) {
                    ObjectNode result = mapper.createObjectNode();
                    result.put("id", id);
                    result.put("name", player.path("name").asText("Unknown"));

                    // Extract team information
                    JsonNode team = scorer.path("team");
                    if (!team.isMissingNode()) {
                        result.put("team", team.path("name").asText("Unknown"));
                    }

                    // Extract scoring statistics
                    int goals = scorer.path("goals").asInt(0);
                    int matches = scorer.path("playedMatches").asInt(1);

                    result.put("goals", goals);
                    result.put("matches", matches);

                    // Calculate performance: goals per match
                    double performance = matches > 0 ? (double) goals / matches : 0.0;
                    result.put("performance", performance);

                    return result;
                }
            }
        }

        return null; // Player not found in this scorers list
    }

    /**
     * Fallback when player is not found in any top scorers list
     *
     * Fetches basic player information from API and formats response
     * to indicate player's performance is below top players.
     *
     * Calls: getPlayerById() from FootballDataService
     *
     * Returns: ObjectNode with basic player info and performance message
     */
    private ObjectNode fetchBasicPlayerInfoAsFallback(String playerId) throws IOException, InterruptedException {
        logger.info("Player {} not found in any competition's top scorers, fetching basic info", playerId);

        JsonNode playerInfo = footballDataService.getPlayerById(playerId);

        ObjectNode response = mapper.createObjectNode();
        response.put("id", Integer.parseInt(playerId));
        response.put("name", playerInfo.path("name").asText("Unknown"));

        // Try to get team info if available
        JsonNode currentTeam = playerInfo.path("currentTeam");
        if (!currentTeam.isMissingNode()) {
            response.put("team", currentTeam.path("name").asText("Unknown"));
        } else {
            response.put("team", "Unknown");
        }

        // Add message indicating player is not in top scorers
        String performanceMessage = String.format(
                "Player %s %s performance is below average top players",
                playerId,
                playerInfo.path("name").asText("Unknown")
        );
        response.put("performance", performanceMessage);

        return response;
    }

    /**
     * Saves player performance data to database for the user
     *
     * Process:
     * 1. Finds existing user record or creates new one
     * 2. Gets existing performance array from database
     * 3. Appends new performance data to array
     * 4. Saves updated array back to database
     *
     * Calls:
     * - findByUserId() from ConsultasRepository
     * - save() from ConsultasRepository
     *
     * Database: Stores in 'consultas' table, 'rendimiento' column as JSON array
     */
    private void savePlayerPerformanceToDatabase(Long userId, ObjectNode newPerformanceData) {
        // Find existing consulta record or create new one
        Consultas consulta = consultasRepository.findByUserId(userId)
                .orElse(new Consultas());

        // Set user ID if it's a new record
        if (consulta.getId() == null) {
            consulta.setUserId(userId);
        }

        try {
            // Get existing performance array or create new one
            ArrayNode performanceArray = getExistingPerformanceArray(consulta);

            // Add new performance data to array
            performanceArray.add(newPerformanceData);

            // Convert array to JSON string and save
            consulta.setRendimiento(mapper.writeValueAsString(performanceArray));
            consultasRepository.save(consulta);

            logger.info("Saved performance data for userId: {}", userId);
        } catch (Exception e) {
            logger.error("Error saving performance data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save performance data", e);
        }
    }

    /**
     * Retrieves existing performance array from database record
     * or creates new empty array if none exists
     *
     * Handles different cases:
     * - Empty/null field: creates new array
     * - Existing array: returns it
     * - Existing object (legacy): converts to array with that object
     *
     * Returns: ArrayNode ready to append new data
     */
    private ArrayNode getExistingPerformanceArray(Consultas consulta) throws IOException {
        String existingRendimiento = consulta.getRendimiento();

        if (existingRendimiento != null && !existingRendimiento.isEmpty()) {
            // Parse existing data
            JsonNode existingNode = mapper.readTree(existingRendimiento);

            if (existingNode.isArray()) {
                // Already an array, return it
                return (ArrayNode) existingNode;
            } else {
                // Legacy: single object, convert to array
                ArrayNode performanceArray = mapper.createArrayNode();
                performanceArray.add(existingNode);
                return performanceArray;
            }
        } else {
            // No existing data, create new array
            return mapper.createArrayNode();
        }
    }

    /**
     * Helper class to hold organized competition lists
     */
    private static class CompetitionLists {
        final List<JsonNode> priorityCompetitions;
        final List<JsonNode> otherCompetitions;

        CompetitionLists(List<JsonNode> priority, List<JsonNode> other) {
            this.priorityCompetitions = priority;
            this.otherCompetitions = other;
        }
    }
}