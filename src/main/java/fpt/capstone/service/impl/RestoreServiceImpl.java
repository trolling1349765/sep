package fpt.capstone.service.impl;

import fpt.capstone.dto.request.RestoreRequest;
import fpt.capstone.dto.response.RestoreResultResponse;
import fpt.capstone.entity.Backup;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.BackupStatus;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BackupRepository;
import fpt.capstone.service.RestoreService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.BackupScope;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates a restore. Deliberately NOT transactional itself: the guards
 * and the CORRUPTED status flip must commit independently, and the DML runs
 * inside RestoreTxExecutor's own transaction. Audit rows go through
 * SystemLogService.write (REQUIRES_NEW) so RESTORE_START/RESTORE_FAILED
 * survive the rollback of a failed restore.
 *
 * Staleness note: no Hibernate L2/query cache is configured, and every request
 * gets a fresh persistence context, so no cache eviction is needed after the
 * bulk JDBC rewrite.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestoreServiceImpl implements RestoreService {

    private final BackupRepository backupRepository;
    private final BackupFileStore fileStore;
    private final RestoreTxExecutor restoreTxExecutor;
    private final SystemLogService systemLogService;
    private final SecurityUtil securityUtil;

    @Override
    public RestoreResultResponse restore(RestoreRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.getConfirm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.CONFIRM_REQUIRED.name());
        }
        if (request.getBackupId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.BACKUP_NOT_FOUND.name());
        }
        Backup backup = backupRepository.findById(request.getBackupId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        ErrorCode.BACKUP_NOT_FOUND.name()));
        if (backup.getStatus() != BackupStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCode.BACKUP_NOT_RESTORABLE.name());
        }

        Path file = backup.getFilePath() == null ? null : Path.of(backup.getFilePath());
        if (file == null || !Files.exists(file)
                || !fileStore.sha256(file).equalsIgnoreCase(backup.getChecksumSha256())) {
            backup.setStatus(BackupStatus.CORRUPTED);
            backupRepository.save(backup);
            throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorCode.BACKUP_CORRUPTED.name());
        }

        Map<String, List<Map<String, Object>>> data = fileStore.read(file).data();
        // A hand-edited file must fail entirely before any DB row is touched.
        Set<String> outOfScope = new HashSet<>(data.keySet());
        outOfScope.removeAll(BackupScope.restorableTables());
        String actorId = securityUtil.getCurrentUserId();
        if (!outOfScope.isEmpty()) {
            writeAudit(Action.RESTORE_FAILED, actorId, backup.getCode(),
                    "Tables outside backup scope: " + outOfScope);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCode.BACKUP_NOT_RESTORABLE.name());
        }

        writeAudit(Action.RESTORE_START, actorId, backup.getCode(),
                "type=" + backup.getType() + ", tables=" + data.size());
        long startedAt = System.currentTimeMillis();
        RestoreTxExecutor.RestoreStats stats;
        try {
            stats = restoreTxExecutor.execute(data);
        } catch (Exception e) {
            log.error("Restore of {} failed, transaction rolled back", backup.getCode(), e);
            writeAudit(Action.RESTORE_FAILED, actorId, backup.getCode(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Restore failed");
        }
        long durationMs = System.currentTimeMillis() - startedAt;

        writeAudit(Action.RESTORE_COMPLETE, actorId, backup.getCode(),
                stats.restoredTables() + " tables, " + stats.restoredRows() + " rows, "
                        + durationMs + " ms");
        return RestoreResultResponse.builder()
                .backupCode(backup.getCode())
                .restoredTables(stats.restoredTables())
                .restoredRows(stats.restoredRows())
                .durationMs(durationMs)
                .build();
    }

    private void writeAudit(Action action, String userId, String backupCode, String detail) {
        try {
            systemLogService.write(SystemLog.builder()
                    .userId(userId)
                    .action(action.getAction())
                    .entityType(Table.BACKUP.getTableName())
                    .entityId(backupCode)
                    .newValue(detail)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write restore audit log for {}", backupCode, e);
        }
    }
}
