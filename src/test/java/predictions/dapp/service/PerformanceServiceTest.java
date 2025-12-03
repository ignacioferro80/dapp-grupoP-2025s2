package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private ConsultasRepository consultasRepository;

    @Mock
    private FootballDataService footballDataService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private PerformanceService performanceService;

    private ObjectMapper mapper;
    private String mockCompetitionsJson;
    private String mockTopScorersJson;
    private String mockPlayerJson;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();

        mockCompetitionsJson = """
            {
              "competitions": [
                {
                  "id": "2021",
                  "name": "Premier League",
                  "code": "PL"
                }
              ]
            }
            """;

        mockTopScorersJson = """
            {
              "scorers": [
                {
                  "player": {
                    "id": 44,
                    "name": "Harry Kane"
                  },
                  "team": {
                    "name": "Tottenham Hotspur FC"
                  },
                  "goals": 25,
                  "playedMatches": 30
                }
              ]
            }
            """;

        mockPlayerJson = """
            {
              "id": 44,
              "name": "Harry Kane",
              "currentTeam": {
                "name": "Tottenham Hotspur FC"
              }
            }
            """;
    }

    @Test
    void handlePerformance_PlayerFoundInTopScorers() throws IOException, InterruptedException {
        // Arrange
        JsonNode competitionsNode = mapper.readTree(mockCompetitionsJson);
        JsonNode topScorersNode = mapper.readTree(mockTopScorersJson);

        // Mock cache to return null (cache miss)
        when(cacheService.getPerformance(anyString())).thenReturn(null);

        when(footballDataService.getCompetitions()).thenReturn(competitionsNode);
        when(footballDataService.getTopScorersByCompetitionId(anyString(), anyInt(), anyString()))
                .thenReturn(topScorersNode);
        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(new Consultas()));
        when(consultasRepository.save(any(Consultas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ObjectNode result = performanceService.handlePerformance(1L, "44");

        // Assert
        assertNotNull(result);
        assertEquals(44, result.get("id").asInt());
        assertEquals("Harry Kane", result.get("name").asText());
        assertEquals(25, result.get("goals").asInt());
        assertEquals(30, result.get("matches").asInt());
        assertTrue(result.has("performance"));

        // Verify cache was checked and updated
        verify(cacheService).getPerformance("44");
        verify(cacheService).cachePerformance(eq("44"), any(ObjectNode.class));
        verify(consultasRepository).save(any(Consultas.class));
    }

    @Test
    void handlePerformance_PlayerNotInTopScorers_FetchesBasicInfo() throws IOException, InterruptedException {
        // Arrange
        JsonNode competitionsNode = mapper.readTree(mockCompetitionsJson);
        JsonNode emptyScorersJson = mapper.readTree("{\"scorers\": []}");
        JsonNode playerNode = mapper.readTree(mockPlayerJson);

        // Mock cache to return null (cache miss)
        when(cacheService.getPerformance(anyString())).thenReturn(null);

        when(footballDataService.getCompetitions()).thenReturn(competitionsNode);
        when(footballDataService.getTopScorersByCompetitionId(anyString(), anyInt(), anyString()))
                .thenReturn(emptyScorersJson);
        when(footballDataService.getPlayerById(anyString())).thenReturn(playerNode);
        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(new Consultas()));
        when(consultasRepository.save(any(Consultas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ObjectNode result = performanceService.handlePerformance(1L, "44");

        // Assert
        assertNotNull(result);
        assertEquals(44, result.get("id").asInt());
        assertEquals("Harry Kane", result.get("name").asText());
        assertTrue(result.get("performance").asText().contains("below average"));

        verify(footballDataService).getPlayerById("44");
        verify(cacheService).getPerformance("44");
        verify(cacheService).cachePerformance(eq("44"), any(ObjectNode.class));
        verify(consultasRepository).save(any(Consultas.class));
    }

    @Test
    void handlePerformance_CachedData_ReturnsFromCache() throws IOException, InterruptedException {
        // Arrange
        ObjectNode cachedData = mapper.createObjectNode();
        cachedData.put("id", 44);
        cachedData.put("name", "Harry Kane");
        cachedData.put("goals", 25);
        cachedData.put("matches", 30);
        cachedData.put("performance", 0.83);

        when(cacheService.getPerformance(anyString())).thenReturn(cachedData);
        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(new Consultas()));
        when(consultasRepository.save(any(Consultas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ObjectNode result = performanceService.handlePerformance(1L, "44");

        // Assert
        assertNotNull(result);
        assertEquals(44, result.get("id").asInt());
        assertEquals("Harry Kane", result.get("name").asText());

        // Verify cache was used and API was NOT called
        verify(cacheService).getPerformance("44");
        verify(footballDataService, never()).getCompetitions();
        verify(footballDataService, never()).getTopScorersByCompetitionId(anyString(), anyInt(), anyString());
        verify(consultasRepository).save(any(Consultas.class));
    }

    @Test
    void handlePerformance_ApiException_ThrowsException() throws IOException, InterruptedException {
        // Arrange
        when(cacheService.getPerformance(anyString())).thenReturn(null);
        when(footballDataService.getCompetitions()).thenThrow(new IOException("API Error"));

        // Act & Assert
        assertThrows(IOException.class, () -> {
            performanceService.handlePerformance(1L, "44");
        });
    }

    @Test
    void handlePerformance_NewConsulta_CreatesNew() throws IOException, InterruptedException {
        // Arrange
        JsonNode competitionsNode = mapper.readTree(mockCompetitionsJson);
        JsonNode topScorersNode = mapper.readTree(mockTopScorersJson);

        when(cacheService.getPerformance(anyString())).thenReturn(null);
        when(footballDataService.getCompetitions()).thenReturn(competitionsNode);
        when(footballDataService.getTopScorersByCompetitionId(anyString(), anyInt(), anyString()))
                .thenReturn(topScorersNode);
        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(consultasRepository.save(any(Consultas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ObjectNode result = performanceService.handlePerformance(1L, "44");

        // Assert
        assertNotNull(result);
        verify(consultasRepository).save(argThat(consulta ->
                consulta.getUserId().equals(1L) && consulta.getRendimiento() != null
        ));
        verify(cacheService).cachePerformance(eq("44"), any(ObjectNode.class));
    }
}