package fpt.capstone.dto.response;

import fpt.capstone.entity.Backup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Every Backup column except file_path (server layout stays private). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResponse {
    private int id;
    private String code;
    private String type;
    private String status;
    private Long sizeBytes;
    private String checksumSha256;
    private Integer tableCount;
    private Long rowCount;
    private String createdBy;
    // Null when createdBy is null (scheduled run) — FE renders "Hệ thống".
    private String createdByName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public static BackupResponse from(Backup backup, String createdByName) {
        return BackupResponse.builder()
                .id(backup.getId())
                .code(backup.getCode())
                .type(backup.getType() == null ? null : backup.getType().name())
                .status(backup.getStatus() == null ? null : backup.getStatus().name())
                .sizeBytes(backup.getSizeBytes())
                .checksumSha256(backup.getChecksumSha256())
                .tableCount(backup.getTableCount())
                .rowCount(backup.getRowCount())
                .createdBy(backup.getCreatedBy())
                .createdByName(createdByName)
                .startedAt(backup.getStartedAt())
                .completedAt(backup.getCompletedAt())
                .errorMessage(backup.getErrorMessage())
                .build();
    }
}
