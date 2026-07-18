package fpt.capstone.repository;

import fpt.capstone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // One-time backfill for rows created before the AccountStatus lifecycle
    // existed (status used to be left NULL). Idempotent.
    @Modifying
    @Query("update User u set u.status = fpt.capstone.enums.AccountStatus.ACTIVE where u.status is null")
    int backfillNullStatusToActive();

    // Aliases must match the getters (Spring Data interface projection).
    interface DashboardUserStats {
        Long getTotal();

        Long getActiveCount();

        Long getPendingVerification();

        Long getLockedCount();

        Long getBannedCount();

        Long getTempLockedCount();
    }

    // Single-round-trip aggregate for the admin dashboard cards. The four
    // headline numbers are independent counts (no sum invariant). Explicit
    // left join: u.role.name inside CASE would generate an inner join and
    // silently drop role-less users from every count.
    @Query("""
            select count(u) as total,
              coalesce(sum(case when u.status in (fpt.capstone.enums.AccountStatus.ACTIVE,
                                                  fpt.capstone.enums.AccountStatus.VERIFIED)
                                 and (u.lockedUntil is null or u.lockedUntil <= :now)
                           then 1 else 0 end), 0) as activeCount,
              coalesce(sum(case when (u.nationalIdVerified is null or u.nationalIdVerified = false)
                                 and r.name = 'Citizen'
                           then 1 else 0 end), 0) as pendingVerification,
              coalesce(sum(case when u.status = fpt.capstone.enums.AccountStatus.BANNED
                                 or u.lockedUntil > :now
                           then 1 else 0 end), 0) as lockedCount,
              coalesce(sum(case when u.status = fpt.capstone.enums.AccountStatus.BANNED
                           then 1 else 0 end), 0) as bannedCount,
              coalesce(sum(case when u.lockedUntil > :now
                                 and (u.status is null or u.status <> fpt.capstone.enums.AccountStatus.BANNED)
                           then 1 else 0 end), 0) as tempLockedCount
            from User u left join u.role r
            """)
    DashboardUserStats countDashboardStats(@Param("now") Instant now);
}
