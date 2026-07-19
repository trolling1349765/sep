package fpt.capstone.config;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.RequestIpUtil;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final SystemLogService systemLogService;
    private final SecurityUtil securityUtil;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void audit(JoinPoint joinPoint, Auditable auditable, Object result) {
        // Audit logging must never break the business operation it observes.
        try {
            systemLogService.write(SystemLog.builder()
                    .userId(safeActorId())
                    .action(auditable.action().getAction())
                    .entityType(auditable.entity().getTableName())
                    .entityId(extractId(result))
                    .ipAddress(RequestIpUtil.getCurrentClientIp())
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Audit log failed for {}: {}", joinPoint.getSignature(), e.getMessage());
        }
    }

    private String safeActorId() {
        try {
            return securityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    // Best effort: use the returned object's getId() when it exposes one.
    private String extractId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
