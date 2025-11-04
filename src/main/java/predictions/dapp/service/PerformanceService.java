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

    public PerformanceService(ConsultasRepository consultasRepository,
                              FootballDataService footballDataService) {
        this.consultasRepository = consultasRepository;
        this.footballDataService = footballDataService;
    }

    @Transactional
    public ObjectNode handlePerformance(Long userId, String playerId) throws IOException, InterruptedException {
        logger.info("Fetching performance data for userId: {} and playerId: {}", userId, playerId);

        // Get all competitions
        JsonNode competitionsResponse = footballDataService.getCompetitions();
        JsonNode competitions = competitionsResponse.path("competitions");

        // Wait after first API call
        Thread.sleep(API_CALL_DELAY_MS);

        ObjectNode playerStats = null;

        // Priority competitions to check first (major leagues)
        List<String> priorityCompetitionIds = List.of("2019", "2021", "2014", "2015", "2002");
        List<JsonNode> priorityComps = new ArrayList<>();
        List<JsonNode> otherComps = new ArrayList<>();

        // Separate priority and other competitions
        if (competitions.isArray()) {
            for (JsonNode competition : competitions) {
                String competitionId = competition.path("id").asText();
                if (priorityCompetitionIds.contains(competitionId)) {
                    priorityComps.add(competition);
                } else {
                    otherComps.add(competition);
                }
            }
        }

        // Search in priority competitions first
        playerStats = searchInCompetitions(priorityComps, playerId);

        // If not found in priority, search in others
        if (playerStats == null) {
            playerStats = searchInCompetitions(otherComps, playerId);
        }

        if (playerStats != null) {
            saveConsulta(userId, playerStats);
            return playerStats;
        }

        // Player not found in any competition's top scorers
        logger.info("Player {} not found in any competition's top scorers, fetching player info", playerId);
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

        response.put("performance", "Player " + playerId + " " + playerInfo.path("name").asText("Unknown") + " performance is below average top players");

        saveConsulta(userId, response);
        return response;
    }

    private ObjectNode searchInCompetitions(List<JsonNode> competitions, String playerId) throws InterruptedException {
        for (JsonNode competition : competitions) {
            String competitionId = competition.path("id").asText();
            String competitionName = competition.path("name").asText();

            logger.info("Checking competition: {} (ID: {})", competitionName, competitionId);

            try {
                // Get top scorers for this competition
                JsonNode topScorers = footballDataService.getTopScorersByCompetitionId(competitionId, 200, "2024");

                // Wait after API call to respect rate limit
                Thread.sleep(API_CALL_DELAY_MS);

                // Search for the player in this competition's top scorers
                ObjectNode playerStats = findPlayerInTopScorers(topScorers, playerId);

                if (playerStats != null) {
                    // Player found in this competition
                    logger.info("Player {} found in competition: {}", playerId, competitionName);
                    playerStats.put("competition", competitionName);
                    return playerStats;
                }
            } catch (IOException e) {
                // Log and continue to next competition if this one fails
                logger.warn("Failed to get scorers for competition {}: {}", competitionName, e.getMessage());

                // If rate limit error, wait longer
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    logger.warn("Rate limit hit, waiting 30 seconds...");
                    Thread.sleep(30000);
                }
                continue;
            }
        }

        return null;
    }

    private ObjectNode findPlayerInTopScorers(JsonNode topScorers, String playerId) {
        JsonNode scorers = topScorers.path("scorers");

        if (scorers.isArray()) {
            for (JsonNode scorer : scorers) {
                JsonNode player = scorer.path("player");
                int id = player.path("id").asInt(-1);

                if (id == Integer.parseInt(playerId)) {
                    ObjectNode result = mapper.createObjectNode();
                    result.put("id", id);
                    result.put("name", player.path("name").asText("Unknown"));

                    // Add team info from the scorer data
                    JsonNode team = scorer.path("team");
                    if (!team.isMissingNode()) {
                        result.put("team", team.path("name").asText("Unknown"));
                    }

                    result.put("goals", scorer.path("goals").asInt(0));
                    result.put("matches", scorer.path("playedMatches").asInt(1));

                    int goals = scorer.path("goals").asInt(0);
                    int matches = scorer.path("playedMatches").asInt(1);
                    double performance = matches > 0 ? (double) goals / matches : 0.0;
                    result.put("performance", performance);

                    return result;
                }
            }
        }

        return null;
    }

    private void saveConsulta(Long userId, ObjectNode newPerformanceData) {
        // Check if there's already a consulta for this user
        Consultas consulta = consultasRepository.findByUserId(userId)
                .orElse(new Consultas());

        // Set user ID if it's a new consulta
        if (consulta.getId() == null) {
            consulta.setUserId(userId);
        }

        try {
            // Get existing rendimiento data or create new array
            ArrayNode performanceArray;
            String existingRendimiento = consulta.getRendimiento();

            if (existingRendimiento != null && !existingRendimiento.isEmpty()) {
                // Parse existing data as array
                JsonNode existingNode = mapper.readTree(existingRendimiento);
                if (existingNode.isArray()) {
                    performanceArray = (ArrayNode) existingNode;
                } else {
                    // If it's an object, convert to array with that object
                    performanceArray = mapper.createArrayNode();
                    performanceArray.add(existingNode);
                }
            } else {
                // Create new array
                performanceArray = mapper.createArrayNode();
            }

            // Add new performance data
            performanceArray.add(newPerformanceData);

            // Save as JSON string
            consulta.setRendimiento(mapper.writeValueAsString(performanceArray));
            consultasRepository.save(consulta);

            logger.info("Saved performance data for userId: {}", userId);
        } catch (Exception e) {
            logger.error("Error saving performance data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save performance data", e);
        }
    }
}