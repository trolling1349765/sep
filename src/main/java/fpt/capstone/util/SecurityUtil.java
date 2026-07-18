package fpt.capstone.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final JwtUtil jwtUtil;

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        return null; // Hoặc throw một Exception tùy bạn thiết kế
    }

    public String getCurrentUserId() {
        String userId = jwtUtil.getUserIdFromRequest(getCurrentRequest());
        return userId;
    }

    /**
     * Whether the current caller carries the given RBAC authority (right code or
     * ROLE_*), as resolved by JwtAuthenticationFilter. Used for `_OWN` ownership
     * checks: callers holding only the `_OWN` variant are restricted to their own
     * records in the service layer.
     */
    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }
}