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
public class SystemLogResponse {
    private int id;
    private String userId;
    private String actorName;
    private String action;
    private String entityType;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static SystemLogResponse from(SystemLog log, String actorName) {
        return SystemLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .actorName(actorName)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
