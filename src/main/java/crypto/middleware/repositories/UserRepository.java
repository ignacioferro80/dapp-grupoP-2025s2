package crypto.middleware.repositories;

import crypto.middleware.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // para que compile findByEmailIgnoreCase(...)
    Optional<User> findByEmailIgnoreCase(String email);

    // útil si querés otra validación
    boolean existsByEmailIgnoreCase(String email);
}
