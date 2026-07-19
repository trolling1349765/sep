package fpt.capstone.service.impl;

import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SystemLogDetailResponse;
import fpt.capstone.dto.response.SystemLogResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.LogSeverity;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements SystemLogService {

    static final int MAX_PAGE_SIZE = 100;

    // Severity filter modes understood by SystemLogRepository.search.
    static final String SEV_NONE = "NONE";
    static final String SEV_IN = "IN";
    static final String SEV_NOT_IN = "NOT_IN";
    // Hibernate cannot expand a null/empty IN-list, so NONE mode ships a dummy.
    static final List<String> NO_SEV_ACTIONS = List.of("__none__");

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;

    @Override
    // Own transaction: audit rows survive a business rollback (login failures,
    // 403 ILLEGAL_REQUEST) and a failed write never rolls back the business op.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SystemLog write(SystemLog systemLog) {
        return systemLogRepository.save(systemLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SystemLogResponse> search(String action, String entityType, String userId,
            LocalDateTime from, LocalDateTime to, String q, String severity, int page, int size) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);

        String pattern = toLikePattern(q);
        String sevMode = SEV_NONE;
        List<String> sevActions = NO_SEV_ACTIONS;
        if (severity != null && !severity.isBlank()) {
            LogSeverity parsed = parseSeverity(severity);
            // INFO is the open-ended default bucket (unknown actions included),
            // so it can only be expressed as NOT IN the two closed buckets.
            switch (parsed) {
                case CRITICAL -> {
                    sevMode = SEV_IN;
                    sevActions = List.copyOf(LogSeverity.criticalActions());
                }
                case WARNING -> {
                    sevMode = SEV_IN;
                    sevActions = List.copyOf(LogSeverity.warningActions());
                }
                case INFO -> {
                    sevMode = SEV_NOT_IN;
                    sevActions = List.copyOf(LogSeverity.nonInfoActions());
                }
            }
        }

        Page<SystemLog> result = systemLogRepository.search(action, entityType, userId, from, to,
                pattern, sevMode, sevActions, PageRequest.of(page, clampedSize));

        Map<String, String> namesById = actorNames(result.getContent());
        return PageResponse.from(result.map(log -> SystemLogResponse.from(log,
                log.getUserId() == null ? null : namesById.get(log.getUserId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public SystemLogDetailResponse getDetail(int id) {
        SystemLog log = systemLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        ErrorCode.LOG_NOT_FOUND.name()));

        String actorName = null;
        String actorRole = null;
        if (log.getUserId() != null) {
            User actor = userRepository.findById(log.getUserId()).orElse(null);
            if (actor != null) {
                actorName = actor.getName();
                actorRole = actor.getRole() == null ? null : actor.getRole().getName();
            }
        }
        return SystemLogDetailResponse.from(log, actorName, actorRole);
    }

    // Lowercased %...% pattern for the JPQL LIKE ... escape '!' guards.
    // '!' is the escape char (not backslash — MySQL treats backslash as its
    // own escape inside string literals, which breaks the rendered SQL).
    private static String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escaped = q.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private static LogSeverity parseSeverity(String severity) {
        try {
            return LogSeverity.valueOf(severity.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
    }

    private Map<String, String> actorNames(List<SystemLog> logs) {
        List<String> actorIds = logs.stream()
                .map(SystemLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return actorIds.isEmpty() ? Map.of()
                : userRepository.findAllById(actorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
    }
}
