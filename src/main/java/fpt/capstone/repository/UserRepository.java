package fpt.capstone.repository;

import fpt.capstone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User getUsersByUsername(String username);

    User findUserByEmail(String email);

    User findUserByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByNationalId(String nationalId);

    boolean existsByUsername(String username);

    User getUserById(String id);

    // Fetch-join role: JwtAuthenticationFilter runs before OSIV, so the LAZY
    // role must be initialized here or authority resolution would fail.
    @Query("select u from User u left join fetch u.role where u.id = :id")
    Optional<User> findWithRoleById(@Param("id") String id);

    Optional<User> findByUsername(String username);

    // Find users with non-null reset token that haven't been used and haven't
    // expired
    // Used for matching hashed reset tokens
    java.util.List<User> findByPasswordResetTokenIsNotNullAndPasswordResetUsedFalseAndPasswordResetTokenExpiryAfter(
            Instant now);
}
