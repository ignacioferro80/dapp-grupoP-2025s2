package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public Map<String, Object> compareTeams(String teamId1, String teamId2)
            throws IOException, InterruptedException {

        // Check cache first
        Map<String, Object> cachedComparison = cacheService.getComparison(teamId1, teamId2);
        if (cachedComparison != null) {
            logger.info("Returning cached comparison for teams {} and {}", teamId1, teamId2);
            return cachedComparison;
        }

        // Obtain stats for both teams
        TeamComparisonStats stats1 = getTeamComparisonStats(teamId1);
        TeamComparisonStats stats2 = getTeamComparisonStats(teamId2);

        // Prepare response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("team1", buildTeamStatsMap(stats1));
        response.put("team2", buildTeamStatsMap(stats2));

        // Cache the comparison result
        cacheService.cacheComparison(teamId1, teamId2, response);

        return response;
    }

    private TeamComparisonStats getTeamComparisonStats(String teamId) 
            throws IOException, InterruptedException {
        TeamComparisonStats stats = new TeamComparisonStats();
        stats.id = teamId;

        // 1. Obtain last 10 matches played
        JsonNode lastMatches = footballDataService.getLastMatchesFinished(teamId, 10);
        calculateMatchStatistics(lastMatches, teamId, stats);

        // 2. Obtain unique leagues/competitions played
        Set<String> leagues = getUniqueLeagues(lastMatches);
        stats.competitions = new ArrayList<>(leagues);

        // 3. For each league, obtain standings
        StandingsData standingsData = processLeagueStandings(leagues, teamId);

        stats.totalPoints = standingsData.totalPoints;
        stats.avgPosition = standingsData.avgPosition;
        stats.goalDifference = standingsData.totalGoalDiff;

        return stats;
    }

    private void calculateMatchStatistics(JsonNode matchesResponse, String teamId, 
                                         TeamComparisonStats stats) {
        JsonNode matches = matchesResponse.get("matches");
        if (matches == null || !matches.isArray()) {
            return;
        }

        int wonGames = 0;
        int totalGoals = 0;
        int matchesPlayed = 0;
        String teamName = "";

        for (JsonNode match : matches) {
            MatchResult result = processMatch(match, teamId);
            if (result.teamName != null && !result.teamName.isEmpty()) {
                teamName = result.teamName;
            }
            if (result.played) {
                matchesPlayed++;
                if (result.won) {
                    wonGames++;
                }
                totalGoals += result.goalsScored;
            }
        }

        stats.name = teamName;
        stats.wonGames = wonGames;
        stats.totalGoals = totalGoals;
        stats.matchesPlayed = matchesPlayed;
    }

    private MatchResult processMatch(JsonNode match, String teamId) {
        JsonNode score = match.get("score");
        if (score == null) {
            return new MatchResult(null, false, 0, false);
        }

        String winner = score.path("winner").asText("");
        JsonNode fullTime = score.get("fullTime");
        if (fullTime == null) {
            return new MatchResult(null, false, 0, false);
        }

        int homeGoals = fullTime.path("home").asInt(0);
        int awayGoals = fullTime.path("away").asInt(0);

        JsonNode homeTeam = match.path("homeTeam");
        JsonNode awayTeam = match.path("awayTeam");

        boolean isHomeTeam = homeTeam.path("id").asText("").equals(teamId);
        boolean isAwayTeam = awayTeam.path("id").asText("").equals(teamId);

        String teamName = null;
        boolean won = false;
        int goalsScored = 0;
        boolean played = false;

        if (isHomeTeam) {
            teamName = homeTeam.path("name").asText("");
            won = "HOME_TEAM".equals(winner);
            goalsScored = homeGoals;
            played = true;
        } else if (isAwayTeam) {
            teamName = awayTeam.path("name").asText("");
            won = "AWAY_TEAM".equals(winner);
            goalsScored = awayGoals;
            played = true;
        }

        return new MatchResult(teamName, won, goalsScored, played);
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

                if (teamStanding != null && !teamStanding.isEmpty()) {
                    return new StandingResult(
                            true,
                            (int) teamStanding.get(POINTS_KEY),
                            (int) teamStanding.get(POSITION_KEY),
                            (int) teamStanding.get(GOAL_DIFFERENCE_KEY)
                    );
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error obteniendo standings para: " + leagueName, e);
        } catch (IOException e) {
            throw new RuntimeException("Error obteniendo standings para: " + leagueName, e);
        }
        return new StandingResult(false, 0, 0, 0);
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

    private Map<String, Object> buildTeamStatsMap(TeamComparisonStats stats) {
        Map<String, Object> teamMap = new LinkedHashMap<>();
        teamMap.put("id", stats.id);
        teamMap.put("name", stats.name);
        teamMap.put("wonGames", stats.wonGames);
        teamMap.put("matchesPlayed", stats.matchesPlayed);
        teamMap.put("totalGoals", stats.totalGoals);
        teamMap.put("totalPoints", stats.totalPoints);
        teamMap.put("avgPosition", stats.avgPosition);
        teamMap.put("goalDifference", stats.goalDifference);
        teamMap.put("competitions", stats.competitions);
        return teamMap;
    }

    // Helper classes
    private static class TeamComparisonStats {
        String id = "";
        String name = "";
        int wonGames = 0;
        int matchesPlayed = 0;
        int totalGoals = 0;
        int totalPoints = 0;
        double avgPosition = 20.0;
        int goalDifference = 0;
        List<String> competitions = new ArrayList<>();
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
        final int goalsScored;
        final boolean played;

        MatchResult(String teamName, boolean won, int goalsScored, boolean played) {
            this.teamName = teamName;
            this.won = won;
            this.goalsScored = goalsScored;
            this.played = played;
        }
    }
}
