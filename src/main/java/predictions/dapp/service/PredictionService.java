package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

import java.io.IOException;
import java.util.*;

@Service
public class PredictionService {

    private final FootballDataService footballDataService;
    private final ConsultasRepository consultasRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public PredictionService(FootballDataService footballDataService,
                             ConsultasRepository consultasRepository) {
        this.footballDataService = footballDataService;
        this.consultasRepository = consultasRepository;
    }

    @Transactional
    public Map<String, Object> predictWinner(String teamId1, String teamId2, Long userId)
            throws Exception {

        // Obtener estadísticas de ambos equipos
        TeamStats stats1 = getTeamStats(teamId1);
        TeamStats stats2 = getTeamStats(teamId2);

        // Calcular probabilidades
        double prob1 = calculateProbability(stats1);
        double prob2 = calculateProbability(stats2);

        // Normalizar probabilidades para que sumen 100%
        double total = prob1 + prob2;
        prob1 = (prob1 / total) * 100;
        prob2 = (prob2 / total) * 100;

        // Determinar ganador
        String winner = prob1 > prob2 ? stats1.teamName : stats2.teamName;
        double winnerProb = Math.max(prob1, prob2);

        // Crear respuesta
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("probabilidad_" + stats1.teamName, String.format("%.2f%%", prob1));
        response.put("probabilidad_" + stats2.teamName, String.format("%.2f%%", prob2));
        response.put("prediction", winner + " con " + String.format("%.2f%%", winnerProb));

        // Guardar en BD (siempre hay userId aquí porque el controller lo valida)
        savePrediction(userId, response);

        return response;
    }

    private TeamStats getTeamStats(String teamId) throws Exception {
        TeamStats stats = new TeamStats();

        // 1. Obtener últimos 10 partidos ganados y goles
        JsonNode lastMatches = footballDataService.getLastMatchesFinished(teamId, 10);
        calculateWinsAndGoals(lastMatches, teamId, stats);

        // 2. Obtener ligas únicas donde jugó
        Set<String> leagues = getUniqueLeagues(lastMatches);

        // 3. Para cada liga, obtener standings
        int totalPoints = 0;
        int totalPosition = 0;
        int totalGoalDiff = 0;
        int leagueCount = 0;

        for (String leagueName : leagues) {
            try {
                // Obtener ID de la competición
                String competitionId = getCompetitionId(leagueName);
                if (competitionId != null) {
                    // Obtener standings
                    JsonNode standings = footballDataService.getStandings(competitionId);
                    Map<String, Object> teamStanding = extractTeamStanding(standings, teamId);

                    if (teamStanding != null) {
                        totalPoints += (int) teamStanding.get("points");
                        totalPosition += (int) teamStanding.get("position");
                        totalGoalDiff += (int) teamStanding.get("goalDifference");
                        leagueCount++;
                    }
                }
            } catch (Exception e) {
                throw new Exception("The temas were not found");
            }
        }

        stats.totalPoints = leagueCount > 0 ? totalPoints : 0;
        stats.avgPosition = leagueCount > 0 ? (double) totalPosition / leagueCount : 20;
        stats.totalGoalDiff = totalGoalDiff;
        stats.leagueCount = leagueCount;

        return stats;
    }

    private void calculateWinsAndGoals(JsonNode matchesResponse, String teamId, TeamStats stats) {
        JsonNode matches = matchesResponse.get("matches");
        if (matches == null || !matches.isArray()) return;

        int wonGames = 0;
        int totalGoals = 0;
        String teamName = "";

        for (JsonNode match : matches) {
            JsonNode score = match.get("score");
            String winner = score.path("winner").asText("");
            JsonNode fullTime = score.get("fullTime");
            if (fullTime == null || score == null) continue;

            int homeGoals = fullTime.path("home").asInt(0);
            int awayGoals = fullTime.path("away").asInt(0);

            // Determinar si este equipo ganó
            JsonNode homeTeam = match.path("homeTeam");
            JsonNode awayTeam = match.path("awayTeam");

            boolean isHomeTeam = homeTeam.path("id").asText("").equals(teamId);
            boolean isAwayTeam = awayTeam.path("id").asText("").equals(teamId);

            if (isHomeTeam) {
                teamName = homeTeam.path("name").asText("");
                if ("HOME_TEAM".equals(winner)) wonGames++;
            } else if (isAwayTeam) {
                teamName = awayTeam.path("name").asText("");
                if ("AWAY_TEAM".equals(winner)) wonGames++;
            }

            // Solo contar goles si el equipo ganó
            if ((isHomeTeam && "HOME_TEAM".equals(winner)) ||
                    (isAwayTeam && "AWAY_TEAM".equals(winner))) {
                totalGoals += homeGoals + awayGoals;
            }
        }

        stats.teamName = teamName;
        stats.wonGames = wonGames;
        stats.goalQuantity = totalGoals;
    }

    private Set<String> getUniqueLeagues(JsonNode matchesResponse) {
        Set<String> leagues = new HashSet<>();
        JsonNode matches = matchesResponse.get("matches");
        if (matches == null || !matches.isArray()) return leagues;

        for (JsonNode match : matches) {
            String leagueName = match.path("competition").path("name").asText("");
            if (!leagueName.isEmpty()) {
                leagues.add(leagueName);
            }
        }
        return leagues;
    }

    private String getCompetitionId(String leagueName) throws IOException, InterruptedException {
        JsonNode competitions = footballDataService.getCompetitions();
        JsonNode competitionsList = competitions.get("competitions");

        if (competitionsList == null || !competitionsList.isArray()) return null;

        for (JsonNode comp : competitionsList) {
            if (leagueName.equals(comp.path("name").asText(""))) {
                return comp.path("id").asText(null);
            }
        }
        return null;
    }

    private Map<String, Object> extractTeamStanding(JsonNode standingsResponse, String teamId) {
        JsonNode standings = standingsResponse.get("standings");
        if (standings == null || !standings.isArray() || standings.size() == 0) return Collections.emptyMap();

        JsonNode table = standings.get(0).get("table");
        if (table == null || !table.isArray()) return Collections.emptyMap();

        for (JsonNode entry : table) {
            if (teamId.equals(entry.path("team").path("id").asText(""))) {
                Map<String, Object> result = new HashMap<>();
                result.put("position", entry.path("position").asInt(0));
                result.put("points", entry.path("points").asInt(0));
                result.put("goalDifference", entry.path("goalDifference").asInt(0));
                return result;
            }
        }
        return Collections.emptyMap();
    }

    private double calculateProbability(TeamStats stats) {
        // Fórmula de probabilidad basada en múltiples factores
        double winRate = stats.wonGames * 10.0; // Máximo 100 puntos (10 victorias)
        double goalScore = Math.min(stats.goalQuantity * 2.0, 100); // Máximo 100 puntos
        double pointsScore = Math.min(stats.totalPoints * 2.0, 100); // Máximo 100 puntos
        double positionScore = Math.max(0, 100 - (stats.avgPosition * 5)); // Mejor posición = más puntos
        double goalDiffScore = Math.min(Math.max(stats.totalGoalDiff * 3, 0), 100); // Máximo 100 puntos

        // Pesos para cada factor
        double probability = (winRate * 0.30) +
                (goalScore * 0.20) +
                (pointsScore * 0.25) +
                (positionScore * 0.15) +
                (goalDiffScore * 0.10);

        return Math.max(probability, 1.0); // Mínimo 1% de probabilidad
    }

    private void savePrediction(Long userId, Map<String, Object> prediction) {
        try {
            Consultas consulta = consultasRepository.findByUserId(userId)
                    .orElse(new Consultas());

            if (consulta.getId() == null) {
                consulta.setUserId(userId);
            }

            // Obtener predicciones existentes
            String existingPredictions = consulta.getPredicciones();
            List<Map<String, Object>> predictionsList = new ArrayList<>();

            if (existingPredictions != null && !existingPredictions.isEmpty()
                    && !existingPredictions.equals("Predictions logged in")) {
                    JsonNode existing = mapper.readTree(existingPredictions);
                    if (existing.isArray()) {
                        for (JsonNode node : existing) {
                            predictionsList.add(mapper.convertValue(node, Map.class));
                        }
                    }
            }

            // Agregar nueva predicción con timestamp
            Map<String, Object> predictionWithTime = new LinkedHashMap<>(prediction);
            predictionWithTime.put("timestamp", new Date().toString());
            predictionsList.add(predictionWithTime);

            // Guardar como JSON array
            consulta.setPredicciones(mapper.writeValueAsString(predictionsList));
            consultasRepository.save(consulta);

        } catch (Exception e) {
            System.err.println("Error guardando predicción: " + e.getMessage());
        }
    }

    // Clase interna para estadísticas del equipo
    private static class TeamStats {
        String teamName = "";
        int wonGames = 0;
        int goalQuantity = 0;
        int totalPoints = 0;
        double avgPosition = 20.0;
        int totalGoalDiff = 0;
        int leagueCount = 0;
    }
}