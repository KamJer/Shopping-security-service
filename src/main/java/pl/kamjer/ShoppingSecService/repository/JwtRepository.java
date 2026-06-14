package pl.kamjer.ShoppingSecService.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.kamjer.ShoppingSecService.model.RefreshToken;

import java.util.Optional;

@Repository
public interface JwtRepository extends JpaRepository<RefreshToken, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.jti = :jti")
    Optional<RefreshToken> findByJtiWithLock(@Param("jti") String jti);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.isRevoked = true WHERE t.user.userName = :userName")
    void revokeAllForUser(@Param("userName") String userName);
}
