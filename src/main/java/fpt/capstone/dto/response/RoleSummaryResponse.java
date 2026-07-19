package fpt.capstone.dto.response;

import fpt.capstone.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleSummaryResponse {

    private int id;
    private String code;
    private String name;
    private String description;
    private long grantedCount;
    private long userCount;

    public static RoleSummaryResponse from(Role role, long grantedCount, long userCount) {
        return RoleSummaryResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .grantedCount(grantedCount)
                .userCount(userCount)
                .build();
    }
}
