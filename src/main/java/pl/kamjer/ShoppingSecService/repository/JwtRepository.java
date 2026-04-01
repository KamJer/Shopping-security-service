package pl.kamjer.ShoppingSecService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.kamjer.ShoppingSecService.model.RefreshToken;

@Repository
public interface JwtRepository extends JpaRepository<RefreshToken, String> {
}
