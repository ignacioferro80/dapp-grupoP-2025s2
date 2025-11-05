package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

import java.io.IOException;
import java.util.*;

@Service
public class PredictionService {

    private static final Logger logger = LoggerFactory.getLogger(PredictionService.class);
    private static final String PERCENTAGE_FORMAT = "%.2f%%";
    private static final String POINTS_KEY = "points";
    private static final String POSITION_KEY = "position";
    private static final String GOAL_DIFFERENCE_KEY = "goalDifference";

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
            throws IOException, InterruptedException {

        // Obtain stats for both teams
        TeamStats stats1 = getTeamStats(teamId1);
        TeamStats stats2 = getTeamStats(teamId2);

        // calculate probabilities
        double prob1 = calculateProbability(stats1);
        double prob2 = calculateProbability(stats2);

        // Round up probabilities to sum 100%
        double total = prob1 + prob2;
        prob1 = (prob1 / total) * 100;
        prob2 = (prob2 / total) * 100;

        // Determine winner
        String winner = prob1 > prob2 ? stats1.teamName : stats2.teamName;
        double winnerProb = Math.max(prob1, prob2);

        // Prepare response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("probabilidad_" + stats1.teamName, String.format(PERCENTAGE_FORMAT, prob1));
        response.put("probabilidad_" + stats2.teamName, String.format(PERCENTAGE_FORMAT, prob2));
        response.put("prediction", winner + " con " + String.format(PERCENTAGE_FORMAT, winnerProb));

        // Save prediction if user is logged in, in db.
        savePrediction(userId, response);

        return response;
    }

    private TeamStats getTeamStats(String teamId) throws IOException, InterruptedException {
        TeamStats stats = new TeamStats();


        // 1  Obtain last 10 matches played
        JsonNode lastMatches = footballDataService.getLastMatchesFinished(teamId, 10);
        calculateWinsAndGoals(lastMatches, teamId, stats);


        // 2  Obtain unique leagues played
        Set<String> leagues = getUniqueLeagues(lastMatches);


        // 3  For each league, obtain standings
        StandingsData standingsData = processLeagueStandings(leagues, teamId);

        stats.totalPoints = standingsData.totalPoints;
        stats.avgPosition = standingsData.avgPosition;
        stats.totalGoalDiff = standingsData.totalGoalDiff;
        stats.leagueCount = standingsData.leagueCount;

        return stats;
    }

    private StandingsData processLeagueStandings(Set<String> leagues, String teamId)
            throws IOException, InterruptedException {
        int totalPoints = 0;
        int totalPosition = 0;
        int totalGoalDiff = 0;
        int leagueCount = 0;

        for (String leagueName : leagues) {
            StandingResult result = processLeagueStanding(leagueName, teamId);
            if (result.found) {
                totalPoints += result.points;
                totalPosition += result.position;
                totalGoalDiff += result.goalDifference;
                leagueCount++;
            }
        }

        double avgPosition = leagueCount > 0 ? (double) totalPosition / leagueCount : 20;
        return new StandingsData(totalPoints, avgPosition, totalGoalDiff, leagueCount);
    }

    private StandingResult processLeagueStanding(String leagueName, String teamId)
            throws IOException, InterruptedException {
        try {
            String competitionId = getCompetitionId(leagueName);
            if (competitionId != null) {
                JsonNode standings = footballDataService.getStandings(competitionId);
                Map<String, Object> teamStanding = extractTeamStanding(standings, teamId);

                if (teamStanding != null) {
                    return new StandingResult(
                            true,
                            (int) teamStanding.get(POINTS_KEY),
                            (int) teamStanding.get(POSITION_KEY),
                            (int) teamStanding.get(GOAL_DIFFERENCE_KEY)
                    );
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error obteniendo standings para {}", leagueName, e);
            throw e;
        }
        return new StandingResult(false, 0, 0, 0);
    }

    private void calculateWinsAndGoals(JsonNode matchesResponse, String teamId, TeamStats stats) {
        JsonNode matches = matchesResponse.get("matches");
        if (matches == null || !matches.isArray()) {
            return;
        }

        int wonGames = 0;
        int totalGoals = 0;
        String teamName = "";

        for (JsonNode match : matches) {
            MatchResult result = processMatch(match, teamId);
            if (result.teamName != null && !result.teamName.isEmpty()) {
                teamName = result.teamName;
            }
            if (result.won) {
                wonGames++;
                totalGoals += result.goals;
            }
        }

        stats.teamName = teamName;
        stats.wonGames = wonGames;
        stats.goalQuantity = totalGoals;
    }

    private MatchResult processMatch(JsonNode match, String teamId) {
        JsonNode score = match.get("score");
        if (score == null) {
            return new MatchResult(null, false, 0);
        }

        String winner = score.path("winner").asText("");
        JsonNode fullTime = score.get("fullTime");
        if (fullTime == null) {
            return new MatchResult(null, false, 0);
        }

        int homeGoals = fullTime.path("home").asInt(0);
        int awayGoals = fullTime.path("away").asInt(0);

        JsonNode homeTeam = match.path("homeTeam");
        JsonNode awayTeam = match.path("awayTeam");

        boolean isHomeTeam = homeTeam.path("id").asText("").equals(teamId);
        boolean isAwayTeam = awayTeam.path("id").asText("").equals(teamId);

        String teamName = null;
        boolean won = false;
        int goals = 0;

        if (isHomeTeam) {
            teamName = homeTeam.path("name").asText("");
            won = "HOME_TEAM".equals(winner);
            if (won) {
                goals = homeGoals + awayGoals;
            }
        } else if (isAwayTeam) {
            teamName = awayTeam.path("name").asText("");
            won = "AWAY_TEAM".equals(winner);
            if (won) {
                goals = homeGoals + awayGoals;
            }
        }

        return new MatchResult(teamName, won, goals);
    }

    private Set<String> getUniqueLeagues(JsonNode matchesResponse) {
        Set<String> leagues = new HashSet<>();
        JsonNode matches = matchesResponse.get("matches");
        if (matches == null || !matches.isArray()) {
            return leagues;
        }

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

        if (competitionsList == null || !competitionsList.isArray()) {
            return null;
        }

        for (JsonNode comp : competitionsList) {
            if (leagueName.equals(comp.path("name").asText(""))) {
                return comp.path("id").asText(null);
            }
        }
        return null;
    }

    private Map<String, Object> extractTeamStanding(JsonNode standingsResponse, String teamId) {
        JsonNode standings = standingsResponse.get("standings");
        if (standings == null || !standings.isArray() || standings.isEmpty()) {
            return Collections.emptyMap();
        }

        JsonNode table = standings.get(0).get("table");
        if (table == null || !table.isArray()) {
            return Collections.emptyMap();
        }

        for (JsonNode entry : table) {
            if (teamId.equals(entry.path("team").path("id").asText(""))) {
                Map<String, Object> result = new HashMap<>();
                result.put(POSITION_KEY, entry.path(POSITION_KEY).asInt(0));
                result.put(POINTS_KEY, entry.path(POINTS_KEY).asInt(0));
                result.put(GOAL_DIFFERENCE_KEY, entry.path(GOAL_DIFFERENCE_KEY).asInt(0));
                return result;
            }
        }
        return Collections.emptyMap();
    }

    private double calculateProbability(TeamStats stats) {
        // Fórmula de probabilidad basada en múltiples factores
        double winRate = stats.wonGames * 10.0;
        double goalScore = Math.min(stats.goalQuantity * 2.0, 100);
        double pointsScore = Math.min(stats.totalPoints * 2.0, 100);
        double positionScore = Math.max(0, 100 - (stats.avgPosition * 5));
        double goalDiffScore = Math.clamp(stats.totalGoalDiff * 3.0, 0, 100);

        // Pesos para cada factor
        double probability = (winRate * 0.30) +
                (goalScore * 0.20) +
                (pointsScore * 0.25) +
                (positionScore * 0.15) +
                (goalDiffScore * 0.10);

        return Math.max(probability, 1.0);
    }

    private void savePrediction(Long userId, Map<String, Object> prediction) {
        savePredictionData(userId, prediction);
    }

    private void savePredictionData(Long userId, Map<String, Object> prediction) {
        try {
            Consultas consulta = consultasRepository.findByUserId(userId)
                    .orElse(new Consultas());

            if (consulta.getId() == null) {
                consulta.setUserId(userId);
            }

            // Obtener predicciones existentes
            List<Map<String, Object>> predictionsList = getExistingPredictions(consulta);

            // Agregar nueva predicción con timestamp
            Map<String, Object> predictionWithTime = new LinkedHashMap<>(prediction);
            predictionWithTime.put("timestamp", new Date().toString());
            predictionsList.add(predictionWithTime);

            // Guardar como JSON array
            consulta.setPredicciones(mapper.writeValueAsString(predictionsList));
            consultasRepository.save(consulta);

        } catch (Exception e) {
            logger.error("Error guardando predicción", e);
        }
    }

    private List<Map<String, Object>> getExistingPredictions(Consultas consulta) {
        List<Map<String, Object>> predictionsList = new ArrayList<>();
        String existingPredictions = consulta.getPredicciones();

        if (existingPredictions != null && !existingPredictions.isEmpty()
                && !"Predictions logged in".equals(existingPredictions)) {
            try {
                JsonNode existing = mapper.readTree(existingPredictions);
                if (existing.isArray()) {
                    for (JsonNode node : existing) {
                        predictionsList.add(mapper.convertValue(node, Map.class));
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not parse existing predictions", e);
            }
        }
        return predictionsList;
    }

    // Helper classes
    private static class TeamStats {
        String teamName = "";
        int wonGames = 0;
        int goalQuantity = 0;
        int totalPoints = 0;
        double avgPosition = 20.0;
        int totalGoalDiff = 0;
        int leagueCount = 0;
    }

    private static class StandingsData {
        final int totalPoints;
        final double avgPosition;
        final int totalGoalDiff;
        final int leagueCount;

        StandingsData(int totalPoints, double avgPosition, int totalGoalDiff, int leagueCount) {
            this.totalPoints = totalPoints;
            this.avgPosition = avgPosition;
            this.totalGoalDiff = totalGoalDiff;
            this.leagueCount = leagueCount;
        }
    }

    private static class StandingResult {
        final boolean found;
        final int points;
        final int position;
        final int goalDifference;

        StandingResult(boolean found, int points, int position, int goalDifference) {
            this.found = found;
            this.points = points;
            this.position = position;
            this.goalDifference = goalDifference;
        }
    }

    private static class MatchResult {
        final String teamName;
        final boolean won;
        final int goals;

        MatchResult(String teamName, boolean won, int goals) {
            this.teamName = teamName;
            this.won = won;
            this.goals = goals;
        }
    }
}