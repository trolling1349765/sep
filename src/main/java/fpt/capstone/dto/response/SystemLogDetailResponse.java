package fpt.capstone.dto.response;

import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.LogSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full detail of one audit-log row. actorRole is the actor's CURRENT role
 * (the log does not store the role at action time); both actorName and
 * actorRole are null when the log has no userId or the user was deleted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogDetailResponse {
    private int id;
    private String userId;
    private String actorName;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String severity;
    private LocalDateTime createdAt;

    public static SystemLogDetailResponse from(SystemLog log, String actorName, String actorRole) {
        return SystemLogDetailResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .actorName(actorName)
                .actorRole(actorRole)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .severity(LogSeverity.of(log.getAction()).name())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
