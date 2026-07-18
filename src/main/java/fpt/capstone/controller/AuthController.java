package fpt.capstone.controller;

import fpt.capstone.dto.request.ChangePasswordRequest;
import fpt.capstone.dto.request.LoginRequest;
import fpt.capstone.dto.request.PasswordResetConfirmRequest;
import fpt.capstone.dto.request.PasswordResetRequest;
import fpt.capstone.dto.request.RegisterRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.LoginResponse;
import fpt.capstone.enums.Action;
import fpt.capstone.service.AuthAuditLogger;
import fpt.capstone.service.AuthService;
import fpt.capstone.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthAuditLogger authAuditLogger;

    @PostMapping("/register")
    public ResponseEntity<APIResponse<LoginResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginResponse result = authService.register(request, httpRequest, httpResponse);
        if (result.getMessage() != null) {
            return ResponseEntity.ok(APIResponse.success(result.getMessage(), null));
        }
        authAuditLogger.logAuthEvent(Action.CREATE_USER, result.getUserId(), httpRequest, "SELF_REGISTER");
        return ResponseEntity.ok(APIResponse.success("Registration successful", result));
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            LoginResponse result = authService.login(request, httpRequest, httpResponse);
            authAuditLogger.logAuthEvent(Action.USER_LOGIN, result.getUserId(), httpRequest, "SUCCESS");
            return ResponseEntity.ok(APIResponse.success("Login successful", result));
        } catch (ResponseStatusException e) {
            // Failed attempts are audited too (actor unknown -> userId null)
            authAuditLogger.logAuthEvent(Action.USER_LOGIN, null, httpRequest, "FAILED");
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                int retryAfter = 60;
                String reason = e.getReason();
                if (reason != null && reason.contains("Retry after")) {
                    try {
                        String[] parts = reason.split("Retry after ");
                        if (parts.length > 1) {
                            retryAfter = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                httpResponse.setHeader("Retry-After", String.valueOf(retryAfter));
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(APIResponse.error(429, e.getReason()));
            }
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<LoginResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        LoginResponse result = authService.refreshAccessToken(request, response);
        return ResponseEntity.ok(APIResponse.success("Token refreshed", result));
    }

    @PostMapping("/logout")
    public ResponseEntity<APIResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String logoutToken = extractAccessToken(request);
        String logoutUserId = (logoutToken != null && jwtUtil.isTokenValid(logoutToken))
                ? jwtUtil.extractUserId(logoutToken) : null;
        authService.logout(request, response);
        authAuditLogger.logAuthEvent(Action.USER_LOGOUT, logoutUserId, request, null);
        return ResponseEntity.ok(APIResponse.success("Logged out successfully", null));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<APIResponse<Void>> logoutAll(
            HttpServletRequest request,
            HttpServletResponse response) {
        String accessToken = extractAccessToken(request);
        if (accessToken != null && jwtUtil.isTokenValid(accessToken) && !jwtUtil.isTokenExpired(accessToken)) {
            String userId = jwtUtil.extractUserId(accessToken);
            authService.logoutAllDevices(userId, response);
            authAuditLogger.logAuthEvent(Action.USER_LOGOUT, userId, request, "ALL_DEVICES");
        } else {
            authService.logout(request, response);
            authAuditLogger.logAuthEvent(Action.USER_LOGOUT, null, request, "ALL_DEVICES");
        }
        return ResponseEntity.ok(APIResponse.success("Logged out from all devices", null));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<APIResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(APIResponse.success(
                "If the email exists, a reset token has been generated.", null));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<APIResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest) {
        authService.confirmPasswordReset(request);
        authAuditLogger.logAuthEvent(Action.PASSWORD_RESET, null, httpRequest, null);
        return ResponseEntity.ok(APIResponse.success("Password reset successful. You can now log in.", null));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('PASSWORD_CHANGE')")
    public ResponseEntity<APIResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        String accessToken = extractAccessToken(httpRequest);
        if (accessToken == null || !jwtUtil.isTokenValid(accessToken) || jwtUtil.isTokenExpired(accessToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        String userId = jwtUtil.extractUserId(accessToken);
        authService.changePassword(userId, request);
        authAuditLogger.logAuthEvent(Action.CHANGE_PASSWORD, userId, httpRequest, null);
        return ResponseEntity.ok(APIResponse.success("Password changed successfully.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<APIResponse<LoginResponse>> me(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        if (accessToken == null || !jwtUtil.isTokenValid(accessToken) || jwtUtil.isTokenExpired(accessToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        String userId = jwtUtil.extractUserId(accessToken);
        String email = jwtUtil.extractEmail(accessToken);
        String role = jwtUtil.extractUserRole(accessToken);

        LoginResponse response = LoginResponse.builder()
                .userId(userId)
                .email(email)
                .role(role)
                .build();
        return ResponseEntity.ok(APIResponse.success(response));
    }

    private String extractAccessToken(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}