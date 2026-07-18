package fpt.capstone.dto.response;

import fpt.capstone.entity.SystemLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private int id;
    private String action;
    private String entityType;
    private String entityId;
    private String actorId;
    private String actorName;
    private String detail;
    private LocalDateTime createdAt;

    public static RecentActivityResponse from(SystemLog log, String actorName) {
        return RecentActivityResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .actorId(log.getUserId())
                .actorName(actorName)
                .detail(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
