package fpt.capstone.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Every 403 (missing authority) is recorded as an ILLEGAL_REQUEST audit row
 * Logging failures must never mask the 403 response itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditingAccessDeniedHandler implements AccessDeniedHandler {

    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.getPrincipal() instanceof User user) ? user.getId() : null;
            systemLogService.write(SystemLog.builder()
                    .userId(userId)
                    .action(Action.ILLEGAL_REQUEST.getAction())
                    .entityType("ENDPOINT")
                    .entityId(request.getMethod() + " " + request.getRequestURI())
                    .ipAddress(RequestIpUtil.getClientIp(request))
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to write ILLEGAL_REQUEST audit log: {}", e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        APIResponse<Void> body = APIResponse.error(ErrorCode.ACCESS_DENIED.getCode(),
                ErrorCode.ACCESS_DENIED.getMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
