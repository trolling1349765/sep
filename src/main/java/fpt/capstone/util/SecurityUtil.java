package fpt.capstone.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
}