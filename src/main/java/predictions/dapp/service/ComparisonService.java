package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import predictions.dapp.exceptions.MetricsException;

import java.io.IOException;
import java.util.*;

@Service
public class ComparisonService {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonService.class);
    private static final String POINTS_KEY = "points";
    private static final String POSITION_KEY = "position";
    private static final String GOAL_DIFFERENCE_KEY = "goalDifference";

    private final FootballDataService footballDataService;
    private final CacheService cacheService;

    public ComparisonService(FootballDataService footballDataService, CacheService cacheService) {
        this.footballDataService = footballDataService;
        this.cacheService = cacheService;
    }

    // ============================================================
    // INTERNAL DEDUPLICATION HELPERS (LOCAL ONLY, NO NEW FILES)
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

        int totalPoints = 0, totalPos = 0, totalGD = 0, count = 0;

        for (String league : leagues) {
            StandingResult res = evaluateLeague(league, teamId);
            if (res.found) {
                totalPoints += res.points;
                totalPos += res.position;
                totalGD += res.goalDifference;
                count++;
            } else {
                logger.warn("Could not find standings for league: {}", league);
            }
        }

        double avgPos = (count > 0) ? (double) totalPos / count : 20;
        return new StandingsData(totalPoints, avgPos, totalGD, count);
    }

    // ============================================================
    // PUBLIC ENTRYPOINT
    // ============================================================

    public Map<String, Object> compareTeams(String teamId1, String teamId2)
            throws IOException, InterruptedException {

        TeamComparisonStats t1 = getStats(teamId1);
        TeamComparisonStats t2 = getStats(teamId2);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("team1", buildMap(t1));
        response.put("team2", buildMap(t2));

        cacheService.cacheComparison(teamId1, teamId2, response);

        return response;
    }

    // ============================================================
    // TEAM STATISTICS PROCESSING
    // ============================================================

    private TeamComparisonStats getStats(String teamId)
            throws IOException, InterruptedException {

        TeamComparisonStats stats = new TeamComparisonStats();
        stats.id = teamId;

        JsonNode lastMatches = footballDataService.getLastMatchesFinished(teamId, 10);
        evaluateMatches(lastMatches, teamId, stats);

        Set<String> leagues = extractLeagues(lastMatches);
        stats.competitions = new ArrayList<>(leagues);

        StandingsData standings = processLeagueStandings(leagues, teamId);

        stats.totalPoints = standings.totalPoints;
        stats.avgPosition = standings.avgPosition;
        stats.totalGoalDiff = standings.totalGoalDiff;
        stats.leagueCount = standings.leagueCount;

        return stats;
    }

    private void evaluateMatches(JsonNode matchesResponse, String teamId, TeamComparisonStats stats) {
        JsonNode matches = safeArray(matchesResponse, "matches");
        if (matches == null) return;

        int won = 0, lost = 0, draw = 0;
        int scored = 0, conceded = 0;
        int played = 0;
        String name = "";

        for (JsonNode match : matches) {
            MatchResult r = parseMatch(match, teamId);

            if (r.teamName != null && !r.teamName.isEmpty()) name = r.teamName;
            if (!r.played) continue;

            played++;
            scored += r.goalsScored;
            conceded += r.goalsConceded;

            if (r.won) won++;
            else if (r.lost) lost++;
            else draw++;
        }

        stats.teamName = name;
        stats.matchesPlayed = played;
        stats.wonGames = won;
        stats.drawnGames = draw;
        stats.lostGames = lost;
        stats.goalsScored = scored;
        stats.goalsConceded = conceded;
    }

    private MatchResult parseMatch(JsonNode match, String teamId) {

        JsonNode score = match.get("score");
        JsonNode full = (score != null) ? score.get("fullTime") : null;

        if (full == null) return new MatchResult(null, false, false, 0, 0, false);

        int home = full.path("home").asInt(0);
        int away = full.path("away").asInt(0);

        JsonNode homeTeam = match.path("homeTeam");
        JsonNode awayTeam = match.path("awayTeam");

        boolean isHome = teamId.equals(homeTeam.path("id").asText(""));
        boolean isAway = teamId.equals(awayTeam.path("id").asText(""));

        if (!isHome && !isAway) return new MatchResult(null, false, false, 0, 0, false);

        String winner = score.path("winner").asText("");

        String name = isHome ? homeTeam.path("name").asText("") : awayTeam.path("name").asText("");
        boolean won = isHome ? "HOME_TEAM".equals(winner) : "AWAY_TEAM".equals(winner);
        boolean lost = isHome ? "AWAY_TEAM".equals(winner) : "HOME_TEAM".equals(winner);

        int gs = isHome ? home : away;
        int gc = isHome ? away : home;

        return new MatchResult(name, won, lost, gs, gc, true);
    }

    // ============================================================
    // OUTPUT MAPPING
    // ============================================================

    private Map<String, Object> buildMap(TeamComparisonStats s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id);
        m.put("name", s.teamName);
        m.put("matchesPlayed", s.matchesPlayed);
        m.put("wonGames", s.wonGames);
        m.put("drawnGames", s.drawnGames);
        m.put("lostGames", s.lostGames);
        m.put("goalsScored", s.goalsScored);
        m.put("goalsConceded", s.goalsConceded);
        m.put("totalPoints", s.totalPoints);
        m.put("avgPosition", s.avgPosition);
        m.put(GOAL_DIFFERENCE_KEY, s.totalGoalDiff);
        m.put("competitions", s.competitions);
        return m;
    }

    // ============================================================
    // INTERNAL CLASSES (unchanged structure)
    // ============================================================

    private static class TeamComparisonStats {
        String id;
        String teamName = "";
        int matchesPlayed, wonGames, lostGames, drawnGames;
        int goalsScored, goalsConceded;
        int totalPoints, totalGoalDiff, leagueCount;
        double avgPosition = 20;
        List<String> competitions = new ArrayList<>();
    }

    private static class StandingsData {
        final int totalPoints;
        final double avgPosition;
        final int totalGoalDiff;
        final int leagueCount;

        StandingsData(int points, double avg, int gd, int count) {
            this.totalPoints = points;
            this.avgPosition = avg;
            this.totalGoalDiff = gd;
            this.leagueCount = count;
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
        final boolean won, lost, played;
        final int goalsScored, goalsConceded;

        MatchResult(String n, boolean w, boolean l, int gs, int gc, boolean played) {
            this.teamName = n;
            this.won = w;
            this.lost = l;
            this.goalsScored = gs;
            this.goalsConceded = gc;
            this.played = played;
        }
    }
}
