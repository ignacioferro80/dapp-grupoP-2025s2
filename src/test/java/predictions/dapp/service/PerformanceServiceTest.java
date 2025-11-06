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
        verify(consultasRepository).save(any(Consultas.class));
    }

    @Test
    void handlePerformance_PlayerNotInTopScorers_FetchesBasicInfo() throws IOException, InterruptedException {
        // Arrange
        JsonNode competitionsNode = mapper.readTree(mockCompetitionsJson);
        JsonNode emptyScorersJson = mapper.readTree("{\"scorers\": []}");
        JsonNode playerNode = mapper.readTree(mockPlayerJson);

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
        verify(consultasRepository).save(any(Consultas.class));
    }

    @Test
    void handlePerformance_ApiException_ThrowsException() throws IOException, InterruptedException {
        // Arrange
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
    }
}