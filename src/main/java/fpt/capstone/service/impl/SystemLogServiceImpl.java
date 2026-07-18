package fpt.capstone.service.impl;

import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SystemLogResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
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
            LocalDateTime from, LocalDateTime to, int page, int size) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCode.ARGUMENT_INVALID.name());
        }
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);

        Page<SystemLog> result = systemLogRepository.search(action, entityType, userId, from, to,
                PageRequest.of(page, clampedSize));

        List<String> actorIds = result.getContent().stream()
                .map(SystemLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> namesById = actorIds.isEmpty() ? Map.of()
                : userRepository.findAllById(actorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        return PageResponse.from(result.map(log -> SystemLogResponse.from(log,
                log.getUserId() == null ? null : namesById.get(log.getUserId()))));
    }
}
