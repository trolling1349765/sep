package fpt.capstone.dto.response;

import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Row of the admin Users table. Deliberately lean — the detail screen uses
 * {@link AdminUserDetailResponse}. Never exposes internal fields (createBy, isDelete...).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {

    private String id;
    private String username;
    private String name;
    private Integer roleId;
    private String roleName;
    private AccountStatus status;
    private boolean tempLocked;
    private String email;

    public static AdminUserListResponse from(User user, Instant now) {
        return AdminUserListResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .status(user.getStatus())
                .tempLocked(user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))
                .email(user.getEmail())
                .build();
    }
}
