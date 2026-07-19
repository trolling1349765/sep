package fpt.capstone.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.capstone.dto.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Deny-by-default with no form/basic login would otherwise surface anonymous
 * access as 403; the spec requires 401 for missing/invalid credentials.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Same wording the controllers used pre-RBAC; asserted by the auth ITs
        APIResponse<Void> body = APIResponse.error(401, "Authentication required.");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
