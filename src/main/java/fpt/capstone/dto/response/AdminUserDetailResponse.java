package fpt.capstone.dto.response;

import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Admin user-detail screen payload. Never contains the password (or its hash)
 * or reset-token internals. createdAt/updatedAt are LocalDate because
 * BaseEntity uses LocalDate (PENDING global migration to LocalDateTime).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private String id;
    private String username;
    private String name;
    private Integer roleId;
    private String roleName;
    private String email;
    private String phone;
    private String position;
    private String assignedArea;
    private AccountStatus status;
    private boolean tempLocked;
    private Instant lockedUntil;
    private Integer failedLoginAttempts;
    private Instant lastLoginAt;
    private LocalDate dob;
    private Boolean gender;
    private Boolean nationalIdVerified;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public static AdminUserDetailResponse from(User user) {
        Instant now = Instant.now();
        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .position(user.getPosition())
                .assignedArea(user.getAssignedArea())
                .status(user.getStatus())
                .tempLocked(user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))
                .lockedUntil(user.getLockedUntil())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .dob(user.getDob())
                .gender(user.getGender())
                .nationalIdVerified(user.getNationalIdVerified())
                .createdAt(user.getCreateAt())
                .updatedAt(user.getUpdateAt())
                .build();
    }
}
