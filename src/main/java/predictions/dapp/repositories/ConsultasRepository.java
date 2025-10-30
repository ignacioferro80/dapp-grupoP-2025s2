package predictions.dapp.repositories;

import predictions.dapp.model.Consultas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultasRepository extends JpaRepository<Consultas, Long> {
    Optional<Consultas> findByUserId(Long userId);
}