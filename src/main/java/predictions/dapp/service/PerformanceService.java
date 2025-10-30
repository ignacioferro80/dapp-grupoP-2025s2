package predictions.dapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import predictions.dapp.model.Consultas;
import predictions.dapp.repositories.ConsultasRepository;

@Service
public class PerformanceService {

    private final ConsultasRepository consultasRepository;

    public PerformanceService(ConsultasRepository consultasRepository) {
        this.consultasRepository = consultasRepository;
    }

    @Transactional
    public String handlePerformance(Long userId) {
        // Check if there's already a consulta for this user
        Consultas consulta = consultasRepository.findByUserId(userId)
                .orElse(new Consultas());

        // Set user ID if it's a new consulta
        if (consulta.getId() == null) {
            consulta.setUserId(userId);
        }

        // Update rendimiento field
        consulta.setRendimiento("Performance logged in");
        consultasRepository.save(consulta);

        return "Performance logged in";
    }
}