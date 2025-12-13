package predictions.dapp.predictions;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import predictions.dapp.exceptions.MetricsException;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;
import predictions.dapp.service.CacheService;
import predictions.dapp.service.FootballDataService;
import predictions.dapp.service.MethodCacheService;
import predictions.dapp.service.PredictionService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PredictionServiceTest {

    @Mock
    private FootballDataService footballDataService;

    @Mock
    private ConsultasRepository consultasRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private MethodCacheService methodCacheService;

    private PredictionService predictionService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        predictionService = new PredictionService(
                footballDataService,
                consultasRepository,
                cacheService,
                methodCacheService
        );
    }

    private ObjectNode createMockMatchesResponse(String teamId, String teamName, int wins, int totalMatches) {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode matches = response.putArray("matches");

        for (int i = 0; i < totalMatches; i++) {
            ObjectNode match = matches.addObject();
            match.put("id", 1000 + i);

            ObjectNode homeTeam = match.putObject("homeTeam");
            homeTeam.put("id", teamId);
            homeTeam.put("name", teamName);

            ObjectNode awayTeam = match.putObject("awayTeam");
            awayTeam.put("id", "999");
            awayTeam.put("name", "Other Team");

            ObjectNode score = match.putObject("score");
            boolean isWin = i < wins;
            score.put("winner", isWin ? "HOME_TEAM" : "AWAY_TEAM");

            ObjectNode fullTime = score.putObject("fullTime");
            fullTime.put("home", isWin ? 3 : 1);
            fullTime.put("away", isWin ? 1 : 3);

            ObjectNode competition = match.putObject("competition");
            competition.put("name", "Premier League");
        }

        return response;
    }

    private ObjectNode createMockCompetitionsResponse() {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode competitions = response.putArray("competitions");

        ObjectNode comp = competitions.addObject();
        comp.put("id", "2021");
        comp.put("name", "Premier League");

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

    // ==================== TEST 1: Method Cache Hit ====================
    @Tag("unit")
    @Test
    void testPredictWinner_MethodCacheHit() throws IOException, InterruptedException {
        Long userId = 1L;
        Map<String, Object> cachedPrediction = Map.of(
                "probabilidad_Arsenal FC", "60.00%",
                "probabilidad_Manchester City FC", "40.00%",
                "prediction", "Arsenal FC con 60.00%"
        );

        when(methodCacheService.getCachedMapResult("predictWinner(86,65)"))
                .thenReturn(Optional.of(cachedPrediction));

        Consultas consulta = new Consultas();
        consulta.setUserId(userId);
        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertEquals(cachedPrediction, result);
        verify(footballDataService, never()).getLastMatchesFinished(anyString(), anyInt());
        verify(methodCacheService).getCachedMapResult("predictWinner(86,65)");
        verify(consultasRepository).save(any(Consultas.class));
    }

    // ==================== TEST 2: Old Cache Hit ====================
    @Tag("unit")
    @Test
    void testPredictWinner_OldCacheHit() throws IOException, InterruptedException {
        Long userId = 1L;
        Map<String, Object> cachedPrediction = Map.of(
                "probabilidad_Arsenal FC", "55.00%",
                "probabilidad_Manchester City FC", "45.00%",
                "prediction", "Arsenal FC con 55.00%"
        );

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction("86", "65")).thenReturn(cachedPrediction);

        Consultas consulta = new Consultas();
        consulta.setUserId(userId);
        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertEquals(cachedPrediction, result);
        verify(footballDataService, never()).getLastMatchesFinished(anyString(), anyInt());
        verify(methodCacheService).cacheResult("predictWinner(86,65)", cachedPrediction);
        verify(consultasRepository).save(any(Consultas.class));
    }

    // ==================== TEST 3: Fresh Calculation - Both Caches Miss ====================
    @Tag("unit")
    @Test
    void testPredictWinner_FreshCalculation() throws IOException, InterruptedException {
        Long userId = 1L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);

        ObjectNode arsenalMatches = createMockMatchesResponse("86", "Arsenal FC", 7, 10);
        ObjectNode cityMatches = createMockMatchesResponse("65", "Manchester City FC", 8, 10);
        ObjectNode competitions = createMockCompetitionsResponse();
        ObjectNode standings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);

        when(footballDataService.getLastMatchesFinished("86", 10)).thenReturn(arsenalMatches);
        when(footballDataService.getLastMatchesFinished("65", 10)).thenReturn(cityMatches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings("2021")).thenReturn(standings);

        Consultas consulta = new Consultas();
        consulta.setUserId(userId);
        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertNotNull(result);
        assertTrue(result.containsKey("probabilidad_Arsenal FC"));
        assertTrue(result.containsKey("probabilidad_Manchester City FC"));
        assertTrue(result.containsKey("prediction"));

        verify(footballDataService).getLastMatchesFinished("86", 10);
        verify(footballDataService).getLastMatchesFinished("65", 10);
        verify(cacheService).cachePrediction(eq("86"), eq("65"), any());
        verify(methodCacheService).cacheResult(eq("predictWinner(86,65)"), any());
        verify(consultasRepository).save(any(Consultas.class));
    }

    // ==================== TEST 4: Team with No Wins ====================
    @Tag("unit")
    @Test
    void testPredictWinner_TeamWithNoWins() throws IOException, InterruptedException {
        Long userId = 1L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);

        ObjectNode team1Matches = createMockMatchesResponse("86", "Weak Team", 0, 10);
        ObjectNode team2Matches = createMockMatchesResponse("65", "Strong Team", 10, 10);
        ObjectNode competitions = createMockCompetitionsResponse();
        ObjectNode standings = createMockStandingsResponse("86", "Weak Team", 18, 5, -20);

        when(footballDataService.getLastMatchesFinished("86", 10)).thenReturn(team1Matches);
        when(footballDataService.getLastMatchesFinished("65", 10)).thenReturn(team2Matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings("2021")).thenReturn(standings);

        Consultas consulta = new Consultas();
        consulta.setUserId(userId);
        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertNotNull(result);
        String prediction = (String) result.get("prediction");
        assertTrue(prediction.contains("Strong Team"), "Strong Team should be predicted as winner");
    }

    // ==================== TEST 5: Empty Matches Response ====================
    @Tag("unit")
    @Test
    void testPredictWinner_EmptyMatches() throws IOException, InterruptedException {
        Long userId = 1L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);

        ObjectNode emptyMatches = mapper.createObjectNode();
        emptyMatches.putArray("matches");

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt())).thenReturn(emptyMatches);

        Consultas consulta = new Consultas();
        consulta.setUserId(userId);
        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertNotNull(result);
        assertTrue(result.containsKey("prediction"));
        verify(footballDataService, never()).getCompetitions();
    }

    // ==================== TEST 6: IOException from Football Data Service ====================
    @Tag("unit")
    @Test
    void testPredictWinner_IOExceptionFromFootballService() throws IOException, InterruptedException {
        Long userId = 1L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);
        when(footballDataService.getLastMatchesFinished(anyString(), anyInt()))
                .thenThrow(new IOException("Network error"));

        assertThrows(IOException.class, () -> {
            predictionService.predictWinner("86", "65", userId);
        });

        verify(consultasRepository, never()).save(any());
    }

    // ==================== TEST 7: MetricsException from Standings ====================
    @Tag("unit")
    @Test
    void testPredictWinner_MetricsExceptionFromStandings() throws IOException, InterruptedException {
        Long userId = 1L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);

        ObjectNode matches = createMockMatchesResponse("86", "Arsenal FC", 5, 10);
        ObjectNode competitions = createMockCompetitionsResponse();

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt())).thenReturn(matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings(anyString())).thenThrow(new IOException("Standings error"));

        assertThrows(MetricsException.class, () -> {
            predictionService.predictWinner("86", "65", userId);
        });
    }

    // ==================== TEST 8: New User - Creates New Consulta ====================
    @Tag("unit")
    @Test
    void testPredictWinner_NewUser() throws IOException, InterruptedException {
        Long userId = 999L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);

        ObjectNode matches = createMockMatchesResponse("86", "Arsenal FC", 5, 10);
        ObjectNode competitions = createMockCompetitionsResponse();
        ObjectNode standings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt())).thenReturn(matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings("2021")).thenReturn(standings);

        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertNotNull(result);
        verify(consultasRepository).save(argThat(consulta ->
                consulta.getUserId().equals(userId)
        ));
    }

    // ==================== TEST 9: Existing User with Previous Predictions ====================
    @Tag("unit")
    @Test
    void testPredictWinner_ExistingUserWithHistory() throws Exception {
        Long userId = 1L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);

        ObjectNode matches = createMockMatchesResponse("86", "Arsenal FC", 5, 10);
        ObjectNode competitions = createMockCompetitionsResponse();
        ObjectNode standings = createMockStandingsResponse("86", "Arsenal FC", 2, 28, 15);

        when(footballDataService.getLastMatchesFinished(anyString(), anyInt())).thenReturn(matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings("2021")).thenReturn(standings);

        String existingPredictions = "[{\"prediction\":\"Team A\",\"timestamp\":\"2024-01-01\"}]";
        Consultas consulta = new Consultas();
        consulta.setId(1L);
        consulta.setUserId(userId);
        consulta.setPredicciones(existingPredictions);

        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertNotNull(result);
        verify(consultasRepository).save(argThat(savedConsulta -> {
            try {
                String predictions = savedConsulta.getPredicciones();
                return predictions.contains("prediction") && predictions.contains("timestamp");
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // ==================== TEST 10: Close Probability Match ====================
    @Tag("unit")
    @Test
    void testPredictWinner_CloseProbabilities() throws IOException, InterruptedException {
        Long userId = 1L;

        when(methodCacheService.getCachedMapResult(anyString())).thenReturn(Optional.empty());
        when(cacheService.getPrediction(anyString(), anyString())).thenReturn(null);

        ObjectNode team1Matches = createMockMatchesResponse("86", "Team A", 5, 10);
        ObjectNode team2Matches = createMockMatchesResponse("65", "Team B", 5, 10);
        ObjectNode competitions = createMockCompetitionsResponse();
        ObjectNode standings1 = createMockStandingsResponse("86", "Team A", 3, 25, 10);
        ObjectNode standings2 = createMockStandingsResponse("65", "Team B", 4, 24, 9);

        when(footballDataService.getLastMatchesFinished("86", 10)).thenReturn(team1Matches);
        when(footballDataService.getLastMatchesFinished("65", 10)).thenReturn(team2Matches);
        when(footballDataService.getCompetitions()).thenReturn(competitions);
        when(footballDataService.getStandings("2021"))
                .thenReturn(standings1)
                .thenReturn(standings2);

        Consultas consulta = new Consultas();
        consulta.setUserId(userId);
        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));

        Map<String, Object> result = predictionService.predictWinner("86", "65", userId);

        assertNotNull(result);

        String prob1 = (String) result.get("probabilidad_Team A");
        String prob2 = (String) result.get("probabilidad_Team B");

        assertNotNull(prob1);
        assertNotNull(prob2);

        double probability1 = Double.parseDouble(prob1.replace("%", ""));
        double probability2 = Double.parseDouble(prob2.replace("%", ""));

        assertEquals(100.0, probability1 + probability2, 0.01, "Probabilities should sum to 100%");
        assertTrue(Math.abs(probability1 - probability2) < 30, "Probabilities should be relatively close");
    }
}