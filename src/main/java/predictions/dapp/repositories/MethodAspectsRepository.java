package predictions.dapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import predictions.dapp.model.MethodAspects;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MethodAspectsRepository extends JpaRepository<MethodAspects, Long> {

    /**
     * Find cached result by method signature that hasn't expired
     */
    @Query("SELECT m FROM MethodAspects m WHERE m.methodSignature = :signature AND m.expiresAt > :now")
    Optional<MethodAspects> findValidCache(@Param("signature") String signature, @Param("now") LocalDateTime now);

    /**
     * Delete all expired cache entries
     */
    @Modifying
    @Query("DELETE FROM MethodAspects m WHERE m.expiresAt <= :now")
    int deleteExpiredEntries(@Param("now") LocalDateTime now);

    /**
     * Find by method signature (for updating existing cache)
     */
    Optional<MethodAspects> findByMethodSignature(String methodSignature);

    /**
     * Count valid (non-expired) cache entries
     */
    @Query("SELECT COUNT(m) FROM MethodAspects m WHERE m.expiresAt > :now")
    long countValidEntries(@Param("now") LocalDateTime now);
}