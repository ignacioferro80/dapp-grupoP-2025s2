package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

@Service
public class HistoryService {

    private static final String PREDICTIONS_KEY = "Predictions";
    private static final String PERFORMANCE_KEY = "Performance";
    private static final String NO_DATA = "No data";

    private final ConsultasRepository consultasRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public HistoryService(ConsultasRepository consultasRepository) {
        this.consultasRepository = consultasRepository;
    }

    public ObjectNode getHistory(Long userId) {
        Consultas consulta = consultasRepository.findByUserId(userId)
                .orElse(null);

        ObjectNode response = mapper.createObjectNode();

        try {
            if (consulta != null) {
                // Handle Predictions
                if (consulta.getPredicciones() != null && !consulta.getPredicciones().isEmpty()) {
                    JsonNode predictionsNode = mapper.readTree(consulta.getPredicciones());
                    response.set(PREDICTIONS_KEY, predictionsNode);
                } else {
                    response.put(PREDICTIONS_KEY, NO_DATA);
                }

                // Handle Performance
                if (consulta.getRendimiento() != null && !consulta.getRendimiento().isEmpty()) {
                    JsonNode performanceNode = mapper.readTree(consulta.getRendimiento());
                    response.set(PERFORMANCE_KEY, performanceNode);
                } else {
                    response.put(PERFORMANCE_KEY, NO_DATA);
                }
            } else {
                response.put(PREDICTIONS_KEY, NO_DATA);
                response.put(PERFORMANCE_KEY, NO_DATA);
            }
        } catch (Exception e) {
            response.put(PREDICTIONS_KEY, "Error parsing data");
            response.put(PERFORMANCE_KEY, "Error parsing data");
        }

        return response;
    }
}