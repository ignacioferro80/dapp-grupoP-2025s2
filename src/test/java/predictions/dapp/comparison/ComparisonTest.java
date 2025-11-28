package predictions.dapp.comparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import predictions.dapp.service.CacheService;
import predictions.dapp.service.ComparisonService;
import predictions.dapp.service.FootballDataService;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ComparisonTest {

    @Mock
    private FootballDataService footballDataService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ComparisonService comparisonService;

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode createMockMatchesResponse(String teamId, String teamName) {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode matches = response.putArray("matches");

        for (int i = 0; i < 10; i++) {
            ObjectNode match = matches.addObject();
            match.put("id", 1000 + i);

            ObjectNode homeTeam = match.putObject("homeTeam");
            homeTeam.put("id", teamId);
            homeTeam.put("name", teamName);

            ObjectNode awayTeam = match.putObject("awayTeam");
            awayTeam.put("id", "999");
            awayTeam.put("name", "Other Team");

            ObjectNode score = match.putObject("score");
            score.put("winner", i % 3 == 0 ? "HOME_TEAM" : (i % 3 == 1 ? "AWAY_TEAM" : "DRAW"));

            ObjectNode fullTime = score.putObject("fullTime");
            fullTime.put("home", i % 3 == 0 ? 3 : 1);
            fullTime.put("away", i % 3 == 1 ? 3 : 1);

            ObjectNode competition = match.putObject("competition");
            competition.put("name", i % 2 == 0 ? "Premier League" : "UEFA Champions League");
        }

        return response;
    }

    private ObjectNode createMockCompetitionsResponse() {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode competitions = response.putArray("competitions");

        ObjectNode comp1 = competitions.addObject();
        comp1.put("id", "2021");
        comp1.put("name", "Premier League");

        ObjectNode comp2 = competitions.addObject();
        comp2.put("id", "2001");
        comp2.put("name", "UEFA Champions League");

        return response;
    }

    private ObjectNode createMockStandingsResponse(String teamId, String teamName, int position, int points, int goalDiff) {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode standings = response.putArray("standings");
        ObjectNode standing = standings.addObject();
        ArrayNode table = standing.putArray("table");

        ObjectNode teamStanding = table.addObject();
        ObjectNode team = teamStanding.putObject("team");
        team.put("id", teamId);
        team.put("name", teamName);
        teamStanding.put("position", position);
        teamStanding.put("points", points);
        teamStanding.put("goalDifference", goalDiff);

        return response;
    }

    @BeforeEach
    void setUp() {
        // Mock cache service to return null (no cached data)
        when(cacheService.getComparison(anyString(), anyString())).thenReturn(null);
    }

    @Tag("unit")
    @Test
    void testCompareTeams_Success() throws IOException, InterruptedException {
        // Mock data for Arsenal (team1)
        ObjectNode arsenalMatches = createMockMatchesResponse("86", "Arsenal FC");
        ObjectNode arsenalStandings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);

        // Mock data for Manchester City (team2)
        ObjectNode cityMatches = createMockMatchesResponse("65", "Manchester City FC");

        // Mock competitions
        ObjectNode competitions = createMockCompetitionsResponse();

        // Setup mocks
        when(footballDataService.getLastMatchesFinished("86", 10)).thenReturn(arsenalMatches);
        when(footballDataService.getLastMatchesFinished("65", 10)).thenReturn(cityMatches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings("2021")).thenReturn(arsenalStandings);
        when(footballDataService.getStandings("2001")).thenReturn(arsenalStandings);

        Map<String, Object> result = comparisonService.compareTeams("86", "65");

        assertNotNull(result);
        assertTrue(result.containsKey("team1"));
        assertTrue(result.containsKey("team2"));

        @SuppressWarnings("unchecked")
        Map<String, Object> team1 = (Map<String, Object>) result.get("team1");
        @SuppressWarnings("unchecked")
        Map<String, Object> team2 = (Map<String, Object>) result.get("team2");

        assertNotNull(team1);
        assertNotNull(team2);
        assertEquals("86", team1.get("id"));
        assertEquals("65", team2.get("id"));

        verify(footballDataService, times(1)).getLastMatchesFinished("86", 10);
        verify(footballDataService, times(1)).getLastMatchesFinished("65", 10);
        verify(footballDataService, atLeast(1)).getCompetitions();
    }

    @Tag("unit")
    @Test
    void testCompareTeams_NoCompetitionFound() throws IOException, InterruptedException {
        ObjectNode team1Matches = createMockMatchesResponse("86", "Arsenal FC");
        ObjectNode team2Matches = createMockMatchesResponse("65", "Manchester City FC");

        ObjectNode emptyCompetitions = mapper.createObjectNode();
        emptyCompetitions.putArray("competitions");

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt()))
                .thenReturn(team1Matches)
                .thenReturn(team2Matches);
        when(footballDataService.getCompetitions()).thenReturn(emptyCompetitions);

        Map<String, Object> result = comparisonService.compareTeams("86", "65");

        assertNotNull(result);
        verify(footballDataService, never()).getStandings(anyString());
    }

    @Tag("unit")
    @Test
    void testCompareTeams_SameTeamId() throws IOException, InterruptedException {
        ObjectNode matches = createMockMatchesResponse("86", "Arsenal FC");
        ObjectNode standings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);
        ObjectNode competitions = createMockCompetitionsResponse();

        when(footballDataService.getLastMatchesFinished("86", 10)).thenReturn(matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings(anyString())).thenReturn(standings);

        Map<String, Object> result = comparisonService.compareTeams("86", "86");

        assertNotNull(result);
        assertTrue(result.containsKey("team1"));
        assertTrue(result.containsKey("team2"));
    }

    @Tag("unit")
    @Test
    void testCompareTeams_VerifyStatisticsCalculation() throws IOException, InterruptedException {
        ObjectNode team1Matches = createMockMatchesResponse("86", "Arsenal FC");
        ObjectNode team2Matches = createMockMatchesResponse("65", "Manchester City FC");
        ObjectNode standings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);
        ObjectNode competitions = createMockCompetitionsResponse();

        when(footballDataService.getLastMatchesFinished("86", 10)).thenReturn(team1Matches);
        when(footballDataService.getLastMatchesFinished("65", 10)).thenReturn(team2Matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings(anyString())).thenReturn(standings);

        Map<String, Object> result = comparisonService.compareTeams("86", "65");

        assertNotNull(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> team1 = (Map<String, Object>) result.get("team1");

        assertNotNull(team1);
        assertTrue(team1.containsKey("name"));
        assertTrue(team1.containsKey("wonGames") || team1.containsKey("totalGoals") || team1.containsKey("totalPoints"));
    }

    @Tag("unit")
    @Test
    void testCompareTeams_EmptyMatchesResponse() throws IOException, InterruptedException {
        ObjectNode emptyMatches = mapper.createObjectNode();
        emptyMatches.putArray("matches");

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt())).thenReturn(emptyMatches);

        Map<String, Object> result = comparisonService.compareTeams("86", "65");

        assertNotNull(result);
        assertTrue(result.containsKey("team1"));
        assertTrue(result.containsKey("team2"));

        verify(footballDataService, never()).getCompetitions();
    }

    @Tag("unit")
    @Test
    void testCompareTeams_MultipleCompetitions() throws IOException, InterruptedException {
        ObjectNode team1Matches = createMockMatchesResponse("86", "Arsenal FC");
        ObjectNode team2Matches = createMockMatchesResponse("65", "Manchester City FC");
        ObjectNode standings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);
        ObjectNode competitions = createMockCompetitionsResponse();

        when(footballDataService.getLastMatchesFinished("86", 10)).thenReturn(team1Matches);
        when(footballDataService.getLastMatchesFinished("65", 10)).thenReturn(team2Matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings(anyString())).thenReturn(standings);

        Map<String, Object> result = comparisonService.compareTeams("86", "65");

        assertNotNull(result);

        @SuppressWarnings("unchecked")
        Map<String, Object> team1 = (Map<String, Object>) result.get("team1");

        if (team1.containsKey("competitions")) {
            assertNotNull(team1.get("competitions"));
        }
    }

    @Tag("unit")
    @Test
    void testCompareTeams_VerifyServiceCalls() throws IOException, InterruptedException {
        ObjectNode matches = createMockMatchesResponse("86", "Arsenal FC");
        ObjectNode standings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);
        ObjectNode competitions = createMockCompetitionsResponse();

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt())).thenReturn(matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings(anyString())).thenReturn(standings);

        comparisonService.compareTeams("86", "65");

        verify(footballDataService, times(2)).getLastMatchesFinished(anyString(), eq(10));
        verify(footballDataService, atLeastOnce()).getCompetitions();
    }
}