package fpt.capstone.controller;

import fpt.capstone.dto.request.UpdateProfileRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.UserProfileResponse;
import fpt.capstone.service.UserProfileService;
import fpt.capstone.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final JwtUtil jwtUtil;

    @PreAuthorize("hasAuthority('PROFILE_VIEW')")
    @GetMapping("/profile")
    public ResponseEntity<APIResponse<UserProfileResponse>> getProfile(HttpServletRequest request) {
        String userId = extractUserIdFromToken(request);
        UserProfileResponse profile = userProfileService.getProfile(userId);
        return ResponseEntity.ok(APIResponse.success("Profile retrieved successfully", profile));
    }

    @PreAuthorize("hasAuthority('PROFILE_UPDATE')")
    @PutMapping("/profile")
    public ResponseEntity<APIResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            HttpServletRequest request) {
        String userId = extractUserIdFromToken(request);
        UserProfileResponse profile = userProfileService.updateProfile(userId, updateRequest);
        return ResponseEntity.ok(APIResponse.success("Profile updated successfully", profile));
    }

    private String extractUserIdFromToken(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        if (accessToken == null || !jwtUtil.isTokenValid(accessToken) || jwtUtil.isTokenExpired(accessToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return jwtUtil.extractUserId(accessToken);
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
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}