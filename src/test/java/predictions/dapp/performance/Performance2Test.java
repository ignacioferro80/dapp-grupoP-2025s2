package predictions.dapp.performance;

import com.fasterxml.jackson.databind.JsonNode;
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
import predictions.dapp.exceptions.PerformanceDataException;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;
import predictions.dapp.service.FootballDataService;
import predictions.dapp.service.PerformanceService;

import java.io.IOException;
import java.util.Optional;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class Performance2Test {

    @Mock
    private ConsultasRepository consultasRepository;

    @Mock
    private FootballDataService footballDataService;

    @InjectMocks
    private PerformanceService performanceService;

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode createMockCompetitionsResponse() {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode competitions = response.putArray("competitions");

        // Priority competitions
        competitions.addObject().put("id", "2019").put("name", "Serie A");
        competitions.addObject().put("id", "2021").put("name", "Premier League");
        competitions.addObject().put("id", "2014").put("name", "La Liga");
        competitions.addObject().put("id", "2015").put("name", "Ligue 1");
        competitions.addObject().put("id", "2002").put("name", "Bundesliga");

        // Other competitions
        competitions.addObject().put("id", "2003").put("name", "Eredivisie");
        competitions.addObject().put("id", "2017").put("name", "Primeira Liga");

        return response;
    }

    private ObjectNode createMockTopScorersResponse(String playerId, boolean includePlayer) {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode scorers = response.putArray("scorers");

        if (includePlayer) {
            ObjectNode scorer = scorers.addObject();
            ObjectNode player = scorer.putObject("player");
            player.put("id", Integer.parseInt(playerId));
            player.put("name", "Test Player");

            ObjectNode team = scorer.putObject("team");
            team.put("name", "Test Team FC");

            scorer.put("goals", 15);
            scorer.put("playedMatches", 12);
        }

        // Add other scorers
        for (int i = 1; i <= 5; i++) {
            ObjectNode scorer = scorers.addObject();
            scorer.putObject("player").put("id", 10000 + i).put("name", "Other Player " + i);
            scorer.putObject("team").put("name", "Team " + i);
            scorer.put("goals", 20 - i);
            scorer.put("playedMatches", 10);
        }

        return response;
    }

    private ObjectNode createMockPlayerResponse(String playerId) {
        ObjectNode player = mapper.createObjectNode();
        player.put("id", Integer.parseInt(playerId));
        player.put("name", "Test Player");
        player.put("nationality", "England");

        ObjectNode currentTeam = player.putObject("currentTeam");
        currentTeam.put("name", "Test Team FC");

        return player;
    }

    @BeforeEach
    void setUp() {
        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(new Consultas()));
        when(consultasRepository.save(any(Consultas.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    // ==================== SUCCESS SCENARIOS ====================

    @Tag("unit")
    @Test
    void testHandlePerformance_PlayerFoundInPriorityCompetition() throws IOException, InterruptedException {
        Long userId = 1L;
        String playerId = "44";

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024"))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals(44, result.get("id").asInt());
        assertEquals("Test Player", result.get("name").asText());
        assertEquals("Test Team FC", result.get("team").asText());
        assertEquals(15, result.get("goals").asInt());
        assertEquals(12, result.get("matches").asInt());
        assertEquals("Serie A", result.get("competition").asText());
        verify(consultasRepository, times(1)).save(any(Consultas.class));
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_PlayerFoundInOtherCompetition() throws IOException, InterruptedException {
        Long userId = 2L;
        String playerId = "99";

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());

        // Not found in priority competitions
        when(footballDataService.getTopScorersByCompetitionId(eq("2019"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));
        when(footballDataService.getTopScorersByCompetitionId(eq("2021"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));
        when(footballDataService.getTopScorersByCompetitionId(eq("2014"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));
        when(footballDataService.getTopScorersByCompetitionId(eq("2015"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));
        when(footballDataService.getTopScorersByCompetitionId(eq("2002"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));

        // Found in other competition
        when(footballDataService.getTopScorersByCompetitionId(eq("2003"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals("Eredivisie", result.get("competition").asText());
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_PlayerNotFound_BasicInfoReturned() throws IOException, InterruptedException {
        Long userId = 3L;
        String playerId = "12345";

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());

        // Not found in any competition
        when(footballDataService.getTopScorersByCompetitionId(anyString(), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));

        when(footballDataService.getPlayerById(playerId)).thenReturn(createMockPlayerResponse(playerId));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals(12345, result.get("id").asInt());
        assertTrue(result.get("performance").asText().contains("below average"));
        verify(footballDataService).getPlayerById(playerId);
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_PlayerWithNoCurrentTeam() throws IOException, InterruptedException {
        Long userId = 4L;
        String playerId = "999";

        ObjectNode playerWithoutTeam = mapper.createObjectNode();
        playerWithoutTeam.put("id", 999);
        playerWithoutTeam.put("name", "Free Agent Player");

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId(anyString(), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));
        when(footballDataService.getPlayerById(playerId)).thenReturn(playerWithoutTeam);

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals("Unknown", result.get("team").asText());
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_HighPerformancePlayer() throws IOException, InterruptedException {
        Long userId = 5L;
        String playerId = "777";

        ObjectNode response = mapper.createObjectNode();
        ArrayNode scorers = response.putArray("scorers");
        ObjectNode scorer = scorers.addObject();
        scorer.putObject("player").put("id", 777).put("name", "Super Striker");
        scorer.putObject("team").put("name", "Goals FC");
        scorer.put("goals", 30);
        scorer.put("playedMatches", 15);

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024")).thenReturn(response);

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals(30, result.get("goals").asInt());
        assertEquals(15, result.get("matches").asInt());
        assertEquals(2.0, result.get("performance").asDouble(), 0.01);
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_PlayerWithZeroGoals() throws IOException, InterruptedException {
        Long userId = 6L;
        String playerId = "888";

        ObjectNode response = mapper.createObjectNode();
        ArrayNode scorers = response.putArray("scorers");
        ObjectNode scorer = scorers.addObject();
        scorer.putObject("player").put("id", 888).put("name", "Defender Player");
        scorer.putObject("team").put("name", "Defense FC");
        scorer.put("goals", 0);
        scorer.put("playedMatches", 20);

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024")).thenReturn(response);

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals(0, result.get("goals").asInt());
        assertEquals(0.0, result.get("performance").asDouble(), 0.01);
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_SavesMultiplePerformanceRecords() throws IOException, InterruptedException {
        Long userId = 7L;
        String playerId = "44";

        Consultas existingConsulta = new Consultas();
        existingConsulta.setId(1L);
        existingConsulta.setUserId(userId);
        ArrayNode existingPerformance = mapper.createArrayNode();
        existingPerformance.addObject().put("id", 123).put("name", "Old Record");
        existingConsulta.setRendimiento(mapper.writeValueAsString(existingPerformance));

        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(existingConsulta));
        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024"))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        performanceService.handlePerformance(userId, playerId);

        verify(consultasRepository).save(argThat(consulta -> {
            try {
                JsonNode performance = mapper.readTree(consulta.getRendimiento());
                return performance.isArray() && performance.size() == 2;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_CreatesNewConsulta() throws IOException, InterruptedException {
        Long userId = 8L;
        String playerId = "44";

        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024"))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        performanceService.handlePerformance(userId, playerId);

        verify(consultasRepository).save(argThat(consulta ->
                consulta.getUserId().equals(userId) && consulta.getRendimiento() != null
        ));
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Tag("unit")
    @Test
    void testHandlePerformance_RateLimitError() throws IOException, InterruptedException {
        Long userId = 11L;
        String playerId = "44";

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId(eq("2019"), anyInt(), anyString()))
                .thenThrow(new IOException("HTTP 429 Rate limit exceeded"));
        when(footballDataService.getTopScorersByCompetitionId(eq("2021"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals("Premier League", result.get("competition").asText());
    }



    @Tag("unit")
    @Test
    void testHandlePerformance_LegacySingleObjectPerformance() throws IOException, InterruptedException {
        Long userId = 14L;
        String playerId = "44";

        Consultas legacyConsulta = new Consultas();
        legacyConsulta.setId(1L);
        legacyConsulta.setUserId(userId);
        ObjectNode singlePerformance = mapper.createObjectNode();
        singlePerformance.put("id", 999);
        singlePerformance.put("name", "Old Record");
        legacyConsulta.setRendimiento(mapper.writeValueAsString(singlePerformance));

        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(legacyConsulta));
        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024"))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        performanceService.handlePerformance(userId, playerId);

        verify(consultasRepository).save(argThat(consulta -> {
            try {
                JsonNode performance = mapper.readTree(consulta.getRendimiento());
                return performance.isArray() && performance.size() == 2;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // ==================== EDGE CASES ====================

    @Tag("unit")
    @Test
    void testHandlePerformance_EmptyCompetitionsList() throws IOException, InterruptedException {
        Long userId = 15L;
        String playerId = "44";

        ObjectNode emptyCompetitions = mapper.createObjectNode();
        emptyCompetitions.putArray("competitions");

        when(footballDataService.getCompetitions()).thenReturn(emptyCompetitions);
        when(footballDataService.getPlayerById(playerId)).thenReturn(createMockPlayerResponse(playerId));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertTrue(result.get("performance").asText().contains("below average"));
        verify(footballDataService, never()).getTopScorersByCompetitionId(anyString(), anyInt(), anyString());
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_EmptyScorersInAllCompetitions() throws IOException, InterruptedException {
        Long userId = 16L;
        String playerId = "44";

        ObjectNode emptyScorers = mapper.createObjectNode();
        emptyScorers.putArray("scorers");

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId(anyString(), anyInt(), anyString()))
                .thenReturn(emptyScorers);
        when(footballDataService.getPlayerById(playerId)).thenReturn(createMockPlayerResponse(playerId));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertTrue(result.get("performance").asText().contains("below average"));
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_PlayerWithSingleMatch() throws IOException, InterruptedException {
        Long userId = 17L;
        String playerId = "555";

        ObjectNode response = mapper.createObjectNode();
        ArrayNode scorers = response.putArray("scorers");
        ObjectNode scorer = scorers.addObject();
        scorer.putObject("player").put("id", 555).put("name", "New Player");
        scorer.putObject("team").put("name", "New Team");
        scorer.put("goals", 1);
        scorer.put("playedMatches", 1);

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024")).thenReturn(response);

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals(1.0, result.get("performance").asDouble(), 0.01);
    }


    @Tag("unit")
    @Test
    void testHandlePerformance_NoTeamInScorerData() throws IOException, InterruptedException {
        Long userId = 19L;
        String playerId = "444";

        ObjectNode response = mapper.createObjectNode();
        ArrayNode scorers = response.putArray("scorers");
        ObjectNode scorer = scorers.addObject();
        scorer.putObject("player").put("id", 444).put("name", "No Team Player");
        scorer.put("goals", 5);
        scorer.put("playedMatches", 10);

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024")).thenReturn(response);

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertFalse(result.has("team"));
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_MixedCompetitionErrors() throws IOException, InterruptedException {
        Long userId = 20L;
        String playerId = "333";

        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());

        // First competition throws error
        when(footballDataService.getTopScorersByCompetitionId(eq("2019"), anyInt(), anyString()))
                .thenThrow(new IOException("Connection failed"));

        // Second competition returns empty
        when(footballDataService.getTopScorersByCompetitionId(eq("2021"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, false));

        // Third competition finds player
        when(footballDataService.getTopScorersByCompetitionId(eq("2014"), anyInt(), anyString()))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        assertEquals("La Liga", result.get("competition").asText());
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_NullRendimientoField() throws IOException, InterruptedException {
        Long userId = 21L;
        String playerId = "44";

        Consultas consulta = new Consultas();
        consulta.setId(1L);
        consulta.setUserId(userId);
        consulta.setRendimiento(null);

        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));
        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024"))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        verify(consultasRepository).save(argThat(c -> {
            try {
                JsonNode performance = mapper.readTree(c.getRendimiento());
                return performance.isArray() && performance.size() == 1;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    @Tag("unit")
    @Test
    void testHandlePerformance_EmptyRendimientoString() throws IOException, InterruptedException {
        Long userId = 22L;
        String playerId = "44";

        Consultas consulta = new Consultas();
        consulta.setId(1L);
        consulta.setUserId(userId);
        consulta.setRendimiento("");

        when(consultasRepository.findByUserId(userId)).thenReturn(Optional.of(consulta));
        when(footballDataService.getCompetitions()).thenReturn(createMockCompetitionsResponse());
        when(footballDataService.getTopScorersByCompetitionId("2019", 200, "2024"))
                .thenReturn(createMockTopScorersResponse(playerId, true));

        ObjectNode result = performanceService.handlePerformance(userId, playerId);

        assertNotNull(result);
        verify(consultasRepository).save(any(Consultas.class));
    }
}