package fpt.capstone.service.impl;

import fpt.capstone.dto.response.BackupOverviewResponse;
import fpt.capstone.dto.response.BackupResponse;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.entity.Backup;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.BackupStatus;
import fpt.capstone.enums.BackupType;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.BackupRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.BackupService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.BackupScope;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_ERROR_LENGTH = 512;
    private static final int CODE_RETRY_ATTEMPTS = 3;
    private static final String APP_VERSION = "0.0.1";

    private final BackupRepository backupRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final BackupFileStore fileStore;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityUtil securityUtil;

    @Value("${app.backup.cron:0 0 2 * * MON}")
    private String cron;

    @Value("${app.backup.scheduled-enabled:true}")
    private boolean scheduledEnabled;

    // Per-JVM guard against concurrent backups (single-instance deployment).
    private final AtomicBoolean backupRunning = new AtomicBoolean(false);

    @Override
    public BackupResponse create(String type) {
        return create(parseType(type), securityUtil.getCurrentUserId());
    }

    @Override
    public BackupResponse create(BackupType type, String createdBy) {
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        if (!backupRunning.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorCode.BACKUP_IN_PROGRESS.name());
        }
        try {
            return doCreate(type, createdBy);
        } finally {
            backupRunning.set(false);
        }
    }

    // Deliberately NOT @Transactional: the dump reads run in autocommit and
    // each repository save commits on its own, so a RUNNING/FAILED row is
    // visible even when the dump itself blows up.
    private BackupResponse doCreate(BackupType type, String createdBy) {
        Backup backup = saveRunningRecord(type, createdBy);
        writeAudit(Action.BACKUP_CREATE, createdBy, backup.getCode(), null);
        try {
            Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
            long rowCount = 0;
            for (String table : BackupScope.tablesFor(type)) {
                // Table names come only from the BackupScope whitelist.
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
                rows.forEach(row -> row.replaceAll((col, value) -> BackupFileStore.normalize(value)));
                data.put(table, rows);
                rowCount += rows.size();
            }

            BackupFileStore.WrittenFile file = fileStore.write(
                    backup.getCode(), type, APP_VERSION, backup.getStartedAt(), data);

            backup.setFilePath(file.path().toString());
            backup.setSizeBytes(file.sizeBytes());
            backup.setChecksumSha256(file.checksumSha256());
            backup.setTableCount(data.size());
            backup.setRowCount(rowCount);
            backup.setStatus(BackupStatus.COMPLETED);
            backup.setCompletedAt(LocalDateTime.now());
            backup = backupRepository.save(backup);

            writeAudit(Action.BACKUP_COMPLETE, createdBy, backup.getCode(),
                    data.size() + " tables, " + rowCount + " rows, " + file.sizeBytes() + " bytes");
            return BackupResponse.from(backup, resolveName(createdBy));
        } catch (Exception e) {
            log.error("Backup {} failed", backup.getCode(), e);
            backup.setStatus(BackupStatus.FAILED);
            backup.setErrorMessage(truncate(e.getMessage()));
            backup.setCompletedAt(LocalDateTime.now());
            backupRepository.save(backup);
            writeAudit(Action.BACKUP_FAILED, createdBy, backup.getCode(), truncate(e.getMessage()));
            // Reason is not an ErrorCode name -> handler falls back to code 500.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Backup failed");
        }
    }

    // The unique constraint on code catches a race between two JVMs; retry
    // with a regenerated sequence instead of failing the whole backup.
    private Backup saveRunningRecord(BackupType type, String createdBy) {
        DataIntegrityViolationException last = null;
        for (int attempt = 0; attempt < CODE_RETRY_ATTEMPTS; attempt++) {
            try {
                return backupRepository.save(Backup.builder()
                        .code(nextCode())
                        .type(type)
                        .status(BackupStatus.RUNNING)
                        .createdBy(createdBy)
                        .startedAt(LocalDateTime.now())
                        .build());
            } catch (DataIntegrityViolationException e) {
                last = e;
            }
        }
        throw last;
    }

    private String nextCode() {
        String prefix = "BK-" + Year.now().getValue() + "-";
        Integer max = backupRepository.findMaxSequenceForPrefix(prefix + "%");
        return prefix + String.format("%03d", (max == null ? 0 : max) + 1);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BackupResponse> list(int page, int size, String status, String type) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);

        Page<Backup> result = backupRepository.search(parseStatus(status), parseType(type),
                PageRequest.of(page, clampedSize));

        Map<String, String> namesById = creatorNames(result.getContent());
        return PageResponse.from(result.map(b -> BackupResponse.from(b,
                b.getCreatedBy() == null ? null : namesById.get(b.getCreatedBy()))));
    }

    @Override
    @Transactional(readOnly = true)
    public BackupOverviewResponse overview() {
        BackupResponse latest = backupRepository.findTopByOrderByStartedAtDescIdDesc()
                .map(b -> BackupResponse.from(b, resolveName(b.getCreatedBy())))
                .orElse(null);

        LocalDateTime nextRunAt = null;
        if (scheduledEnabled) {
            try {
                nextRunAt = CronExpression.parse(cron).next(LocalDateTime.now());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid backup cron '{}', nextRunAt unavailable", cron);
            }
        }
        return BackupOverviewResponse.builder()
                .schedule(new BackupOverviewResponse.ScheduleInfo(scheduledEnabled, cron, nextRunAt))
                .latest(latest)
                .build();
    }

    private BackupType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return BackupType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
    }

    private BackupStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return BackupStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
    }

    private String resolveName(String userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(User::getName).orElse(null);
    }

    private Map<String, String> creatorNames(List<Backup> backups) {
        List<String> ids = backups.stream()
                .map(Backup::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return ids.isEmpty() ? Map.of()
                : userRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
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
            // Audit failure must never break the backup itself.
            log.error("Failed to write backup audit log for {}", backupCode, e);
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
