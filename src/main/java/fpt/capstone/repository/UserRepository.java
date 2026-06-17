package fpt.capstone.repository;

import fpt.capstone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    User getUsersByUsername(String username);
    User findUserByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    User getUserById(String id);
}
