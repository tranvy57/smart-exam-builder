package iuh.fit.smartexambuilderbe.repositories;

import iuh.fit.smartexambuilderbe.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findUserByUsername(String username);
    Boolean existsByUsername(String username);
}
