package fpt.capstone.entity;

import fpt.capstone.enums.BackupStatus;
import fpt.capstone.enums.BackupType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "backups")
public class Backup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    // BK-<yyyy>-<3-digit seq>; the unique constraint is the race guard for
    // concurrent code generation.
    @Column(name = "code", length = 32, unique = true, nullable = false)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16, nullable = false)
    BackupType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    BackupStatus status;

    // Server-side path; never exposed through the API.
    @Column(name = "file_path", length = 512)
    String filePath;

    @Column(name = "size_bytes")
    Long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    String checksumSha256;

    @Column(name = "table_count")
    Integer tableCount;

    @Column(name = "row_count")
    Long rowCount;

    // Null when created by the scheduled job (FE renders "Hệ thống").
    @Column(name = "created_by", length = 36)
    String createdBy;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @Column(name = "error_message", length = 512)
    String errorMessage;
}
