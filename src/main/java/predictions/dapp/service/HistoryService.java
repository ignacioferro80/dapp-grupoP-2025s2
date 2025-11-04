package predictions.dapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

@Service
public class HistoryService {

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
                    response.set("Predictions", predictionsNode);
                } else {
                    response.put("Predictions", "No data");
                }

                // Handle Performance
                if (consulta.getRendimiento() != null && !consulta.getRendimiento().isEmpty()) {
                    JsonNode performanceNode = mapper.readTree(consulta.getRendimiento());
                    response.set("Performance", performanceNode);
                } else {
                    response.put("Performance", "No data");
                }
            } else {
                response.put("Predictions", "No data");
                response.put("Performance", "No data");
            }
        } catch (Exception e) {
            response.put("Predictions", "Error parsing data");
            response.put("Performance", "Error parsing data");
        }

        return response;
    }
}