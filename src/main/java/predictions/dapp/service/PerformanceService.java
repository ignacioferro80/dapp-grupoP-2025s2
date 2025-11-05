package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.exceptions.PerformanceDataException;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceService.class);
    private static final String UNKNOWN_VALUE = "Unknown";

    private final ConsultasRepository consultasRepository;
    private final FootballDataService footballDataService;
    private final ObjectMapper mapper = new ObjectMapper();

    // Delay between API calls in milliseconds (6 seconds = 10 requests per minute max)
    private static final long API_CALL_DELAY_MS = 6000;
    private static final long RATE_LIMIT_DELAY_MS = 30000;

    // Major football leagues to check first
    private static final List<String> PRIORITY_COMPETITION_IDS = List.of("2019", "2021", "2014", "2015", "2002");

    public PerformanceService(ConsultasRepository consultasRepository,
                              FootballDataService footballDataService) {
        this.consultasRepository = consultasRepository;
        this.footballDataService = footballDataService;
    }

    @Transactional
    public ObjectNode handlePerformance(Long userId, String playerId) throws IOException, InterruptedException {
        if (logger.isInfoEnabled()) {
            logger.info("Fetching performance data for userId: {} and playerId: [PROTECTED]", userId);
        }

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

    private JsonNode fetchAllCompetitions() throws IOException, InterruptedException {
        JsonNode competitionsResponse = footballDataService.getCompetitions();
        JsonNode competitions = competitionsResponse.path("competitions");

        // Wait to respect API rate limit
        Thread.sleep(API_CALL_DELAY_MS);

        return competitions;
    }

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

    private ObjectNode searchInCompetitions(List<JsonNode> competitions, String playerId) throws InterruptedException {
        for (JsonNode competition : competitions) {
            String competitionId = competition.path("id").asText();
            String competitionName = competition.path("name").asText();

            if (logger.isInfoEnabled()) {
                logger.info("Checking competition: {} (ID: {})", competitionName, competitionId);
            }

            try {
                // Get top 200 scorers for this competition in 2024 season
                JsonNode topScorers = footballDataService.getTopScorersByCompetitionId(competitionId, 200, "2024");

                // Wait after API call to respect rate limit
                Thread.sleep(API_CALL_DELAY_MS);

                // Search for the player in this competition's top scorers
                ObjectNode playerStats = findPlayerInTopScorers(topScorers, playerId);

                if (playerStats != null) {
                    // Player found! Add competition name and return
                    if (logger.isInfoEnabled()) {
                        logger.info("Player found in competition: {}", competitionName);
                    }
                    playerStats.put("competition", competitionName);
                    return playerStats;
                }
            } catch (IOException e) {
                // Log error and continue to next competition
                logger.warn("Failed to get scorers for competition {}: {}", competitionName, e.getMessage());

                // If rate limit error (HTTP 429), wait longer before continuing
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    logger.warn("Rate limit hit, waiting 30 seconds...");
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                }
            }
        }

        return null; // Player not found in any of these competitions
    }

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
                    result.put("name", player.path("name").asText(UNKNOWN_VALUE));

                    // Extract team information
                    JsonNode team = scorer.path("team");
                    if (!team.isMissingNode()) {
                        result.put("team", team.path("name").asText(UNKNOWN_VALUE));
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

    private ObjectNode fetchBasicPlayerInfoAsFallback(String playerId) throws IOException, InterruptedException {
        if (logger.isInfoEnabled()) {
            logger.info("Player not found in any competition's top scorers, fetching basic info");
        }

        JsonNode playerInfo = footballDataService.getPlayerById(playerId);

        ObjectNode response = mapper.createObjectNode();
        response.put("id", Integer.parseInt(playerId));
        response.put("name", playerInfo.path("name").asText(UNKNOWN_VALUE));

        // Try to get team info if available
        JsonNode currentTeam = playerInfo.path("currentTeam");
        if (!currentTeam.isMissingNode()) {
            response.put("team", currentTeam.path("name").asText(UNKNOWN_VALUE));
        } else {
            response.put("team", UNKNOWN_VALUE);
        }

        // Add message indicating player is not in top scorers
        String performanceMessage = String.format(
                "Player %s %s performance is below average top players",
                playerId,
                playerInfo.path("name").asText(UNKNOWN_VALUE)
        );
        response.put("performance", performanceMessage);

        return response;
    }

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
            logger.error("Error saving performance data", e);
            throw new PerformanceDataException("Failed to save performance data", e);
        }
    }

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
        }
        // No existing data, create new array
        return mapper.createArrayNode();
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