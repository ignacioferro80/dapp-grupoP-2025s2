package predictions.dapp.service;

import org.springframework.stereotype.Service;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

import java.util.HashMap;
import java.util.Map;

@Service
public class HistoryService {

    private final ConsultasRepository consultasRepository;

    public HistoryService(ConsultasRepository consultasRepository) {
        this.consultasRepository = consultasRepository;
    }

    public Map<String, String> getHistory(Long userId) {
        Consultas consulta = consultasRepository.findByUserId(userId)
                .orElse(null);

        Map<String, String> response = new HashMap<>();

        if (consulta != null) {
            response.put("Predictions", consulta.getPredicciones() != null ? consulta.getPredicciones() : "No data");
            response.put("Performance", consulta.getRendimiento() != null ? consulta.getRendimiento() : "No data");
        } else {
            response.put("Predictions", "No data");
            response.put("Performance", "No data");
        }

        return response;
    }
}