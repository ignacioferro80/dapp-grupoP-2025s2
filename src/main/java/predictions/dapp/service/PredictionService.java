package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.exceptions.MetricsException;
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

    // ============================================================
    // INTERNAL DEDUPLICATION HELPERS
    // ============================================================

    private JsonNode safeArray(JsonNode parent, String field) {
        JsonNode arr = parent.get(field);
        return (arr != null && arr.isArray()) ? arr : null;
    }

    private String safeText(JsonNode node, String field) {
        return node != null ? node.path(field).asText("") : "";
    }

    private int safeInt(JsonNode node, String field) {
        return node != null ? node.path(field).asInt(0) : 0;
    }

    private Set<String> extractLeagues(JsonNode matchesResponse) {
        Set<String> leagues = new HashSet<>();
        JsonNode matches = safeArray(matchesResponse, "matches");
        if (matches == null) return leagues;

        for (JsonNode match : matches) {
            String league = match.path("competition").path("name").asText("");
            if (!league.isEmpty()) leagues.add(league);
        }
        return leagues;
    }

    private String getCompetitionId(String leagueName) throws IOException, InterruptedException {
        JsonNode competitions = footballDataService.getCompetitions();
        JsonNode list = safeArray(competitions, "competitions");
        if (list == null) return null;

        for (JsonNode comp : list) {
            if (leagueName.equals(safeText(comp, "name"))) {
                return safeText(comp, "id");
            }
        }
        return null;
    }

    private Map<String, Object> extractTeamStanding(JsonNode standingsResponse, String teamId) {
        JsonNode standings = safeArray(standingsResponse, "standings");
        if (standings == null || standings.isEmpty()) return Collections.emptyMap();

        JsonNode table = safeArray(standings.get(0), "table");
        if (table == null) return Collections.emptyMap();

        for (JsonNode entry : table) {
            if (teamId.equals(entry.path("team").path("id").asText(""))) {
                Map<String, Object> out = new HashMap<>();
                out.put(POSITION_KEY, safeInt(entry, POSITION_KEY));
                out.put(POINTS_KEY, safeInt(entry, POINTS_KEY));
                out.put(GOAL_DIFFERENCE_KEY, safeInt(entry, GOAL_DIFFERENCE_KEY));
                return out;
            }
        }
        return Collections.emptyMap();
    }

    private StandingResult evaluateLeague(String leagueName, String teamId)
            throws IOException, InterruptedException {

        try {
            String competitionId = getCompetitionId(leagueName);
            if (competitionId != null) {
                JsonNode standings = footballDataService.getStandings(competitionId);
                Map<String, Object> standing = extractTeamStanding(standings, teamId);
                if (!standing.isEmpty()) {
                    return new StandingResult(
                            true,
                            (int) standing.get(POINTS_KEY),
                            (int) standing.get(POSITION_KEY),
                            (int) standing.get(GOAL_DIFFERENCE_KEY)
                    );
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MetricsException("Error obteniendo standings para: " + leagueName, e);
        } catch (IOException e) {
            throw new MetricsException("Error obteniendo standings para: " + leagueName, e);
        }

        return new StandingResult(false, 0, 0, 0);
    }

    private StandingsData processLeagueStandings(Set<String> leagues, String teamId)
            throws IOException, InterruptedException {

        int totalPoints = 0;
        int totalPos = 0;
        int  totalGD = 0;
        int  count = 0;

        for (String league : leagues) {
            StandingResult res = evaluateLeague(league, teamId);
            if (res.found) {
                totalPoints += res.points;
                totalPos += res.position;
                totalGD += res.goalDifference;
                count++;
            }
        }

        double avgPos = (count > 0) ? (double) totalPos / count : 20;
        return new StandingsData(totalPoints, avgPos, totalGD, count);
    }

    // ============================================================
    // PUBLIC API
    // ============================================================

    @Transactional
    public Map<String, Object> predictWinner(String teamId1, String teamId2, Long userId)
            throws IOException, InterruptedException {

        TeamStats stats1 = getStats(teamId1);
        TeamStats stats2 = getStats(teamId2);

        double prob1 = calculateProbability(stats1);
        double prob2 = calculateProbability(stats2);

        double total = prob1 + prob2;
        prob1 = (prob1 / total) * 100;
        prob2 = (prob2 / total) * 100;

        String winner = (prob1 > prob2) ? stats1.teamName : stats2.teamName;
        double winnerProb = Math.max(prob1, prob2);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("probabilidad_" + stats1.teamName, String.format(PERCENTAGE_FORMAT, prob1));
        response.put("probabilidad_" + stats2.teamName, String.format(PERCENTAGE_FORMAT, prob2));
        response.put("prediction", winner + " con " + String.format(PERCENTAGE_FORMAT, winnerProb));

        savePrediction(userId, response);

        return response;
    }

    // ============================================================
    // TEAM DATA PROCESSING
    // ============================================================

    private TeamStats getStats(String teamId) throws IOException, InterruptedException {
        TeamStats stats = new TeamStats();

        JsonNode matches = footballDataService.getLastMatchesFinished(teamId, 10);
        evaluateMatches(matches, teamId, stats);

        Set<String> leagues = extractLeagues(matches);
        StandingsData sd = processLeagueStandings(leagues, teamId);

        stats.totalPoints = sd.totalPoints;
        stats.avgPosition = sd.avgPosition;
        stats.totalGoalDiff = sd.totalGoalDiff;
        stats.leagueCount = sd.leagueCount;

        return stats;
    }

    private void evaluateMatches(JsonNode matchesResponse, String teamId, TeamStats stats) {
        JsonNode matches = safeArray(matchesResponse, "matches");
        if (matches == null) return;

        int won = 0;
        int goals = 0;
        String name = "";

        for (JsonNode match : matches) {
            MatchResult r = parseMatch(match, teamId);
            if (r.teamName != null && !r.teamName.isEmpty()) name = r.teamName;
            if (r.won) {
                won++;
                goals += r.goals;
            }
        }

        stats.teamName = name;
        stats.wonGames = won;
        stats.goalQuantity = goals;
    }

    private MatchResult parseMatch(JsonNode match, String teamId) {

        JsonNode score = match.get("score");
        JsonNode full = (score != null) ? score.get("fullTime") : null;

        if (full == null) return new MatchResult(null, false, 0);

        int home = full.path("home").asInt(0);
        int away = full.path("away").asInt(0);

        JsonNode homeTeam = match.path("homeTeam");
        JsonNode awayTeam = match.path("awayTeam");

        boolean isHome = teamId.equals(homeTeam.path("id").asText(""));
        boolean isAway = teamId.equals(awayTeam.path("id").asText(""));

        if (!isHome && !isAway) return new MatchResult(null, false, 0);

        String winner = safeText(score, "winner");
        String name = isHome ? homeTeam.path("name").asText("") : awayTeam.path("name").asText("");

        boolean won = isHome ? "HOME_TEAM".equals(winner) : "AWAY_TEAM".equals(winner);
        int goals = won ? (home + away) : 0;

        return new MatchResult(name, won, goals);
    }

    // ============================================================
    // PROBABILITY + SAVING
    // ============================================================

    private double calculateProbability(TeamStats stats) {
        double winRate = stats.wonGames * 10.0;
        double goalScore = Math.min(stats.goalQuantity * 2.0, 100);
        double pointsScore = Math.min(stats.totalPoints * 2.0, 100);
        double positionScore = Math.max(0, 100 - (stats.avgPosition * 5));
        double goalDiffScore = Math.clamp(stats.totalGoalDiff * 3.0, 0, 100);

        return Math.max(
                (winRate * 0.30) +
                        (goalScore * 0.20) +
                        (pointsScore * 0.25) +
                        (positionScore * 0.15) +
                        (goalDiffScore * 0.10),
                1.0
        );
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

            List<Map<String, Object>> predictionsList = getExistingPredictions(consulta);

            Map<String, Object> predictionWithTime = new LinkedHashMap<>(prediction);
            predictionWithTime.put("timestamp", new Date().toString());
            predictionsList.add(predictionWithTime);

            consulta.setPredicciones(mapper.writeValueAsString(predictionsList));
            consultasRepository.save(consulta);

        } catch (Exception e) {
            logger.error("Error guardando predicción", e);
        }
    }

    private List<Map<String, Object>> getExistingPredictions(Consultas consulta) {
        List<Map<String, Object>> list = new ArrayList<>();
        String existing = consulta.getPredicciones();

        if (existing != null && !existing.isEmpty()
                && !"Predictions logged in".equals(existing)) {
            try {
                JsonNode arr = mapper.readTree(existing);
                if (arr.isArray()) {
                    for (JsonNode n : arr) {
                        list.add(mapper.convertValue(n, Map.class));
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not parse existing predictions", e);
            }
        }
        return list;
    }

    // ============================================================
    // INTERNAL CLASSES (unchanged)
    // ============================================================

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

        StandingsData(int p, double a, int gd, int c) {
            this.totalPoints = p;
            this.avgPosition = a;
            this.totalGoalDiff = gd;
            this.leagueCount = c;
        }
    }

    private static class StandingResult {
        final boolean found;
        final int points;
        final int position;
        final int goalDifference;

        StandingResult(boolean f, int p, int pos, int gd) {
            this.found = f;
            this.points = p;
            this.position = pos;
            this.goalDifference = gd;
        }
    }

    private static class MatchResult {
        final String teamName;
        final boolean won;
        final int goals;

        MatchResult(String n, boolean w, int g) {
            this.teamName = n;
            this.won = w;
            this.goals = g;
        }
    }
}
