package fpt.capstone.repository;

import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

    // Update-time uniqueness: exclude the row being edited.
    boolean existsByEmailAndIdNot(String email, String id);

    boolean existsByPhoneAndIdNot(String phone, String id);

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

    // Admin user list. No `order by` here: the dynamic whitelisted Sort inside the
    // Pageable is appended by Spring Data (an inline order by would break that).
    // Left join fetch keeps role-less users; countQuery uses the FK path (no join).
    @Query(value = """
            select u from User u left join fetch u.role
            where (:q is null
                   or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.name) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%')))
              and (:roleId is null or u.role.id = :roleId)
              and (:status is null or u.status = :status)
            """,
            countQuery = """
            select count(u) from User u
            where (:q is null
                   or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.name) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%')))
              and (:roleId is null or u.role.id = :roleId)
              and (:status is null or u.status = :status)
            """)
    Page<User> search(@Param("q") String q,
                      @Param("roleId") Integer roleId,
                      @Param("status") AccountStatus status,
                      Pageable pageable);

    // Aliases must match the getters (Spring Data interface projection).
    interface RoleUserCount {
        Integer getRoleId();

        long getUserCount();
    }

    // Single group-by for the Roles screen "Số người dùng" column — never count per role.
    @Query("select u.role.id as roleId, count(u) as userCount from User u where u.role is not null group by u.role.id")
    List<RoleUserCount> countUsersGroupedByRole();
}
