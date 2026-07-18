package fpt.capstone.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Client IP resolution for audit logging (same header precedence as the
 * existing rate-limiter lookups: X-Forwarded-For, X-Real-IP, remote address).
 */
public final class RequestIpUtil {

    private RequestIpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /** IP of the request bound to the current thread, or null outside a request. */
    public static String getCurrentClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return getClientIp(attributes.getRequest());
        }
        return null;
    }
}
