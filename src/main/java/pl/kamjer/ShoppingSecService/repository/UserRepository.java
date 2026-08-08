package pl.kamjer.ShoppingSecService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.kamjer.ShoppingSecService.model.User;
import pl.kamjer.ShoppingSecService.service.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserName(String username);
    boolean existsByUserName(String userName);
    long countByRole(Role role);
    List<User> findAllByOrderByUserNameAsc();
}
