package fpt.capstone.service;

import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;
import fpt.capstone.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthAuditLogger {

    private final SystemLogService systemLogService;

    public void logAuthEvent(Action action, String userId, HttpServletRequest request, String detail) {
        try {
            systemLogService.write(SystemLog.builder()
                    .userId(userId)
                    .action(action.getAction())
                    .entityType(Table.USER.getTableName())
                    .entityId(userId)
                    .newValue(detail)
                    .ipAddress(request != null ? RequestIpUtil.getClientIp(request) : null)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write {} audit log: {}", action, e.getMessage());
        }
    }
}
