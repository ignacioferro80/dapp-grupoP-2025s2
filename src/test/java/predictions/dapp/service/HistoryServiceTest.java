package predictions.dapp.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private ConsultasRepository consultasRepository;

    @InjectMocks
    private HistoryService historyService;

    private Consultas consultas;

    @BeforeEach
    void setUp() {
        consultas = new Consultas();
        consultas.setId(1L);
        consultas.setUserId(1L);
    }

    @Test
    void getHistory_WithData_ReturnsHistory() {
        // Arrange
        String predictionsJson = """
            [
              {
                "probabilidad_Arsenal": "65.42%",
                "probabilidad_Chelsea": "34.58%",
                "prediction": "Arsenal con 65.42%"
              }
            ]
            """;
        String performanceJson = """
            [
              {
                "id": 44,
                "name": "Harry Kane",
                "goals": 25
              }
            ]
            """;

        consultas.setPredicciones(predictionsJson);
        consultas.setRendimiento(performanceJson);

        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(consultas));

        // Act
        ObjectNode result = historyService.getHistory(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.has("Predictions"));
        assertTrue(result.has("Performance"));
        assertTrue(result.get("Predictions").isArray());
        assertTrue(result.get("Performance").isArray());
        verify(consultasRepository).findByUserId(1L);
    }

    @Test
    void getHistory_NoData_ReturnsNoData() {
        // Arrange
        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        // Act
        ObjectNode result = historyService.getHistory(1L);

        // Assert
        assertNotNull(result);
        assertEquals("No data", result.get("Predictions").asText());
        assertEquals("No data", result.get("Performance").asText());
        verify(consultasRepository).findByUserId(1L);
    }

    @Test
    void getHistory_EmptyFields_ReturnsNoData() {
        // Arrange
        consultas.setPredicciones("");
        consultas.setRendimiento(null);

        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(consultas));

        // Act
        ObjectNode result = historyService.getHistory(1L);

        // Assert
        assertNotNull(result);
        assertEquals("No data", result.get("Predictions").asText());
        assertEquals("No data", result.get("Performance").asText());
    }

    @Test
    void getHistory_InvalidJson_ReturnsError() {
        // Arrange
        consultas.setPredicciones("invalid json{");
        consultas.setRendimiento("also invalid}");

        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(consultas));

        // Act
        ObjectNode result = historyService.getHistory(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Error parsing data", result.get("Predictions").asText());
        assertEquals("Error parsing data", result.get("Performance").asText());
    }

    @Test
    void getHistory_OnlyPredictions_ReturnsPartialData() {
        // Arrange
        String predictionsJson = """
            [{"prediction": "Team A wins"}]
            """;

        consultas.setPredicciones(predictionsJson);
        consultas.setRendimiento(null);

        when(consultasRepository.findByUserId(anyLong())).thenReturn(Optional.of(consultas));

        // Act
        ObjectNode result = historyService.getHistory(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.get("Predictions").isArray());
        assertEquals("No data", result.get("Performance").asText());
    }
}