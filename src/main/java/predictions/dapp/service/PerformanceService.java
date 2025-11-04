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

@Service
public class PerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceService.class);
    private final ConsultasRepository consultasRepository;
    private final FootballDataService footballDataService;
    private final ObjectMapper mapper = new ObjectMapper();

    public PerformanceService(ConsultasRepository consultasRepository,
                              FootballDataService footballDataService) {
        this.consultasRepository = consultasRepository;
        this.footballDataService = footballDataService;
    }

    @Transactional
    public ObjectNode handlePerformance(Long userId, String playerId) throws IOException, InterruptedException {
        logger.info("Fetching performance data for userId: {} and playerId: {}", userId, playerId);

        // Get top 50 scorers from Serie A
        JsonNode topScorers = footballDataService.getTopScorers("SA", 50, "2024");

        // Search for the player in top 50
        ObjectNode playerStats = findPlayerInTopScorers(topScorers, playerId);

        if (playerStats != null) {
            // Player is in top 50
            logger.info("Player {} found in top 50 scorers", playerId);
            saveConsulta(userId, playerStats);
            return playerStats;
        } else {
            // Player not in top 50, fetch player info
            logger.info("Player {} not in top 50, fetching player info", playerId);
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

            response.put("performance", "Not in the top 50 players, performance below average");

            saveConsulta(userId, response);
            return response;
        }
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