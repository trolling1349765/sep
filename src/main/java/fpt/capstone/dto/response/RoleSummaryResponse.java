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
    private String name;
    private String description;
    private long grantedCount;

    public static RoleSummaryResponse from(Role role, long grantedCount) {
        return RoleSummaryResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .grantedCount(grantedCount)
                .build();
    }
}
