package fpt.capstone.repository;

import fpt.capstone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User getUsersByUsername(String username);

    User findUserByEmail(String email);

    User findUserByNationalId(String nationalId);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);

    boolean existsByUsername(String username);

    User getUserById(String id);

    Optional<User> findByUsername(String username);
}