package predictions.dapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

@Service
public class PredictionService {

    private final ConsultasRepository consultasRepository;

    public PredictionService(ConsultasRepository consultasRepository) {
        this.consultasRepository = consultasRepository;
    }

    @Transactional
    public String handlePrediction(Long userId) {
        // Check if there's already a consulta for this user
        Consultas consulta = consultasRepository.findByUserId(userId)
                .orElse(new Consultas());

        // Set user ID if it's a new consulta
        if (consulta.getId() == null) {
            consulta.setUserId(userId);
        }

        // Update predicciones field
        consulta.setPredicciones("Predictions logged in");
        consultasRepository.save(consulta);

        return "Predictions logged in";
    }
}