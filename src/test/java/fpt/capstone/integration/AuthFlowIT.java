package fpt.capstone.integration;

import fpt.capstone.entity.RefreshToken;
import fpt.capstone.entity.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIT extends AbstractIntegrationTest {

        private MvcResult performRegister(String email, String phone, String password, String confirmation)
                        throws Exception {
                String body = """
                                {
                                  "fullName": "IT User",
                                  "dateOfBirth": "01/01/2000",
                                  "email": "%s",
                                  "phone": "%s",
                                  "password": "%s",
                                  "passwordConfirmation": "%s"
                                }
                                """.formatted(email, phone, password, confirmation);
                return mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andReturn();
        }

        private MvcResult performChangePassword(Cookie accessCookie, String current, String newPw, String confirm)
                        throws Exception {
                String body = """
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s",
                                  "confirmNewPassword": "%s"
                                }
                                """.formatted(current, newPw, confirm);
                var request = post("/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body);
                if (accessCookie != null) {
                        request = request.cookie(accessCookie);
                }
                return mockMvc.perform(request).andReturn();
        }

        private MvcResult performPasswordResetRequest(String email) throws Exception {
                return mockMvc.perform(post("/auth/password-reset/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\": \"%s\"}".formatted(email)))
                                .andReturn();
        }

        private MvcResult performPasswordResetConfirm(String otp, String newPassword) throws Exception {
                String body = """
                                {
                                  "resetToken": "%s",
                                  "newPassword": "%s"
                                }
                                """.formatted(otp, newPassword);
                return mockMvc.perform(post("/auth/password-reset/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andReturn();
        }

        // ============================================================
        // REGISTER
        // ============================================================
        @Nested
        class RegisterTests {

                @Test
                void register_success() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        String phone = uniquePhone();
                        String body = """
                                        {
                                          "fullName": "IT User",
                                          "dateOfBirth": "01/01/2000",
                                          "email": "%s",
                                          "phone": "%s",
                                          "password": "%s",
                                          "passwordConfirmation": "%s"
                                        }
                                        """.formatted(email, phone, DEFAULT_PASSWORD, DEFAULT_PASSWORD);

                        // Act
                        MvcResult result = mockMvc.perform(post("/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                        // Assert - envelope + cookies
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("Registration successful"))
                                        .andExpect(jsonPath("$.data.userId").isNotEmpty())
                                        .andExpect(jsonPath("$.data.email").value(email))
                                        .andExpect(jsonPath("$.data.role").value("Citizen"))
                                        .andExpect(jsonPath("$.data.accessTokenExpiresAt").isNotEmpty())
                                        .andExpect(cookie().exists("access_token"))
                                        .andExpect(cookie().exists("refresh_token"))
                                        .andExpect(cookie().httpOnly("access_token", true))
                                        .andExpect(cookie().httpOnly("refresh_token", true))
                                        .andExpect(cookie().path("access_token", "/"))
                                        .andExpect(cookie().maxAge("access_token", 900))
                                        .andExpect(cookie().maxAge("refresh_token", 604800))
                                        .andReturn();

                        // Assert - persisted state
                        User saved = userRepository.findUserByEmail(email);
                        assertNotNull(saved);
                        assertTrue(saved.getPassword().startsWith("$2a$12$"));
                        assertEquals(email, saved.getUsername());
                        // role is LAZY since the RBAC change - reload with fetch-join
                        assertEquals("Citizen", userRepository.findWithRoleById(saved.getId())
                                        .orElseThrow().getRole().getName());

                        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(saved.getId());
                        assertEquals(1, active.size());
                        assertTrue(active.get(0).getToken().startsWith("$2"),
                                        "refresh token must be stored BCrypt-hashed");
                        assertTrue(passwordEncoder.matches(
                                        refreshTokenRaw(cookieOf(result, "refresh_token")),
                                        active.get(0).getToken()));
                }

                @Test
                void register_invalidEmail_returns400() throws Exception {
                        // Act & Assert - single invalid field so the first-field-error handler is deterministic
                        String body = """
                                        {
                                          "fullName": "IT User",
                                          "dateOfBirth": "01/01/2000",
                                          "email": "not-an-email",
                                          "phone": "%s",
                                          "password": "%s",
                                          "passwordConfirmation": "%s"
                                        }
                                        """.formatted(uniquePhone(), DEFAULT_PASSWORD, DEFAULT_PASSWORD);
                        mockMvc.perform(post("/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(400))
                                        .andExpect(jsonPath("$.message").value("Email must be a valid format."))
                                        .andExpect(jsonPath("$.data.email").value("not-an-email"));
                }

                @Test
                void register_invalidPhone_returns400() throws Exception {
                        // Act & Assert
                        String body = """
                                        {
                                          "fullName": "IT User",
                                          "dateOfBirth": "01/01/2000",
                                          "email": "%s",
                                          "phone": "12345",
                                          "password": "%s",
                                          "passwordConfirmation": "%s"
                                        }
                                        """.formatted(uniqueEmail(), DEFAULT_PASSWORD, DEFAULT_PASSWORD);
                        mockMvc.perform(post("/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value(400))
                                        .andExpect(jsonPath("$.message")
                                                        .value("Phone must be exactly 10 digits starting with 0."))
                                        .andExpect(jsonPath("$.data.phone").value("12345"));
                }

                @Test
                void register_passwordMismatch_returns400() throws Exception {
                        // Arrange
                        String email = uniqueEmail();

                        // Act
                        MvcResult result = performRegister(email, uniquePhone(), DEFAULT_PASSWORD, "Password2");

                        // Assert
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Password and password confirmation do not match."));
                        assertNull(userRepository.findUserByEmail(email), "user must not be created");
                }

                @Test
                void register_duplicateEmail_returns409() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());

                        // Act
                        MvcResult result = performRegister(email, uniquePhone(), DEFAULT_PASSWORD, DEFAULT_PASSWORD);

                        // Assert
                        assertEquals(409, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString().contains("Email already exists."));
                }

                @Test
                void register_duplicatePhone_returns409() throws Exception {
                        // Arrange
                        String phone = uniquePhone();
                        registerUser(uniqueEmail(), phone);

                        // Act
                        MvcResult result = performRegister(uniqueEmail(), phone, DEFAULT_PASSWORD, DEFAULT_PASSWORD);

                        // Assert
                        assertEquals(409, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString().contains("Phone number already exists."));
                }

                @Test
                void register_weakPassword_returns400() throws Exception {
                        // Act - valid length but no uppercase (service-level policy, not bean validation)
                        MvcResult result = performRegister(uniqueEmail(), uniquePhone(), "password1", "password1");

                        // Assert
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Password must contain at least 1 uppercase letter and 1 number."));
                }
        }

        // ============================================================
        // LOGIN
        // ============================================================
        @Nested
        class LoginTests {

                @Test
                void login_byEmail_success() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());

                        // Act
                        MvcResult result = loginAs(email, DEFAULT_PASSWORD, uniqueIp());

                        // Assert
                        assertEquals(200, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString().contains("Login successful"));
                        Cookie refreshCookie = cookieOf(result, "refresh_token");
                        assertNotNull(cookieOf(result, "access_token"));
                        assertNotNull(refreshCookie);

                        // regression for the raw-token bug: the login session's refresh token
                        // must be stored BCrypt-hashed and match the raw cookie value
                        RefreshToken loginToken = refreshTokenRepository.findById(refreshTokenId(refreshCookie))
                                        .orElseThrow();
                        assertTrue(loginToken.getToken().startsWith("$2"),
                                        "login must store the refresh token BCrypt-hashed");
                        assertTrue(passwordEncoder.matches(refreshTokenRaw(refreshCookie), loginToken.getToken()));

                        User user = userRepository.findUserByEmail(email);
                        assertEquals(0, user.getFailedLoginAttempts());
                        assertNotNull(user.getLastLoginAt());
                }

                @Test
                void login_byPhone_success() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        String phone = uniquePhone();
                        registerUser(email, phone);

                        // Act
                        MvcResult result = loginAs(phone, DEFAULT_PASSWORD, uniqueIp());

                        // Assert
                        assertEquals(200, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString().contains(email));
                }

                @Test
                void login_wrongPassword_returns401() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());

                        // Act
                        MvcResult result = loginAs(email, "WrongPassword1", uniqueIp());

                        // Assert - generic message, no field disclosure
                        assertEquals(401, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Incorrect username or password. Please check again."));
                        assertEquals(1, userRepository.findUserByEmail(email).getFailedLoginAttempts());
                }

                @Test
                void login_unknownCredential_returns401() throws Exception {
                        // Act - same generic message as wrong password (no user enumeration)
                        MvcResult result = loginAs(uniqueEmail(), DEFAULT_PASSWORD, uniqueIp());

                        // Assert
                        assertEquals(401, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Incorrect username or password. Please check again."));
                }

                @Test
                void login_fiveFailures_locksAccount_returns423() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());
                        String ipA = uniqueIp();

                        // Act - 5 failed attempts from IP-A (exactly the per-IP bucket size)
                        for (int i = 0; i < 5; i++) {
                                assertEquals(401, loginAs(email, "WrongPassword1", ipA).getResponse().getStatus());
                        }

                        // Assert - lock is set on the 5th failure
                        User locked = userRepository.findUserByEmail(email);
                        assertEquals(5, locked.getFailedLoginAttempts());
                        assertNotNull(locked.getLockedUntil());

                        // Act - 6th attempt with the CORRECT password from a fresh IP bucket
                        MvcResult result = loginAs(email, DEFAULT_PASSWORD, uniqueIp());

                        // Assert
                        assertEquals(423, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Your account is temporarily locked. Please try again later."));
                }

                @Test
                void login_sixthAttemptSameIp_returns429WithRetryAfter() throws Exception {
                        // Arrange - unknown credential: cheap requests (no BCrypt), no lock interference
                        String credential = uniqueEmail();
                        String ip = uniqueIp();

                        // Act - 5 allowed attempts consume the bucket
                        for (int i = 0; i < 5; i++) {
                                assertEquals(401, loginAs(credential, DEFAULT_PASSWORD, ip).getResponse().getStatus());
                        }

                        // Assert - 6th from the same IP is rate limited
                        mockMvc.perform(post("/auth/login")
                                        .header("X-Forwarded-For", ip)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"credential\": \"%s\", \"password\": \"%s\"}"
                                                        .formatted(credential, DEFAULT_PASSWORD)))
                                        .andExpect(status().isTooManyRequests())
                                        .andExpect(header().exists("Retry-After"))
                                        .andExpect(jsonPath("$.code").value(429))
                                        .andExpect(jsonPath("$.message", startsWith("Too many login attempts")));
                }
        }

        // ============================================================
        // /auth/me
        // ============================================================
        @Nested
        class MeTests {

                @Test
                void me_withValidAccessCookie_returnsIdentity() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        String userId = userIdFromBody(registered);

                        // Act & Assert
                        mockMvc.perform(get("/auth/me").cookie(cookieOf(registered, "access_token")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.data.userId").value(userId))
                                        .andExpect(jsonPath("$.data.email").value(email))
                                        .andExpect(jsonPath("$.data.role").value("Citizen"));
                }

                @Test
                void me_withoutCookie_returns401() throws Exception {
                        mockMvc.perform(get("/auth/me"))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.code").value(401))
                                        .andExpect(jsonPath("$.message").value("Authentication required."));
                }

                @Test
                void me_withGarbageCookie_returns401() throws Exception {
                        mockMvc.perform(get("/auth/me").cookie(new Cookie("access_token", "not-a-jwt")))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.message").value("Authentication required."));
                }

                @Test
                void me_withBearerHeaderOnly_returns401() throws Exception {
                        // Arrange - a perfectly valid token, but sent as a Bearer header
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());
                        String token = cookieOf(registered, "access_token").getValue();

                        // Act & Assert - /auth/* endpoints are cookie-only (unlike /users/*)
                        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                                        .andExpect(status().isUnauthorized());
                }
        }

        // ============================================================
        // REFRESH
        // ============================================================
        @Nested
        class RefreshTests {

                @Test
                void refresh_afterRegister_rotatesToken() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());
                        Cookie oldRefresh = cookieOf(registered, "refresh_token");
                        long oldId = refreshTokenId(oldRefresh);

                        // Act
                        MvcResult result = mockMvc.perform(post("/auth/refresh").cookie(oldRefresh))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Token refreshed"))
                                        .andExpect(cookie().exists("access_token"))
                                        .andExpect(cookie().exists("refresh_token"))
                                        .andReturn();

                        // Assert - rotation
                        Cookie newRefresh = cookieOf(result, "refresh_token");
                        assertNotEquals(oldRefresh.getValue(), newRefresh.getValue());

                        RefreshToken oldToken = refreshTokenRepository.findById(oldId).orElseThrow();
                        assertTrue(oldToken.isRevoked(), "old token must be revoked after rotation");

                        RefreshToken newToken = refreshTokenRepository.findById(refreshTokenId(newRefresh))
                                        .orElseThrow();
                        assertFalse(newToken.isRevoked());
                        assertEquals(oldToken.getFamilyId(), newToken.getFamilyId());
                        assertTrue(newToken.getToken().startsWith("$2"),
                                        "rotated token must be stored BCrypt-hashed");
                }

                @Test
                void refresh_afterLogin_succeeds() throws Exception {
                        // Arrange - regression test for the raw-token storage bug:
                        // before the fix, refresh after LOGIN always failed 401 and revoked the family
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());
                        MvcResult login = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        assertEquals(200, login.getResponse().getStatus());

                        // Act & Assert
                        mockMvc.perform(post("/auth/refresh").cookie(cookieOf(login, "refresh_token")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Token refreshed"));
                }

                @Test
                void refresh_secondRotation_succeeds() throws Exception {
                        // Arrange - regression: before the fix the rotated token was stored raw,
                        // so the 2nd refresh always failed
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());
                        MvcResult first = mockMvc.perform(
                                        post("/auth/refresh").cookie(cookieOf(registered, "refresh_token")))
                                        .andExpect(status().isOk())
                                        .andReturn();

                        // Act & Assert
                        mockMvc.perform(post("/auth/refresh").cookie(cookieOf(first, "refresh_token")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Token refreshed"));
                }

                @Test
                void refresh_reuseOfRotatedToken_revokesFamily() throws Exception {
                        // Arrange - rotate once, then replay the ORIGINAL cookie
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());
                        Cookie originalRefresh = cookieOf(registered, "refresh_token");
                        String familyId = refreshTokenRepository.findById(refreshTokenId(originalRefresh))
                                        .orElseThrow().getFamilyId();
                        mockMvc.perform(post("/auth/refresh").cookie(originalRefresh))
                                        .andExpect(status().isOk());

                        // Act & Assert - reuse detection
                        mockMvc.perform(post("/auth/refresh").cookie(originalRefresh))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.message")
                                                        .value("Token reuse detected. All sessions revoked."))
                                        .andExpect(cookie().maxAge("access_token", 0))
                                        .andExpect(cookie().maxAge("refresh_token", 0));

                        // Assert - the whole family is dead
                        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(familyId);
                        assertFalse(family.isEmpty());
                        assertTrue(family.stream().allMatch(RefreshToken::isRevoked));
                }

                @Test
                void refresh_withoutCookie_returns401() throws Exception {
                        mockMvc.perform(post("/auth/refresh"))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.message").value("No valid refresh token."));
                }

                @Test
                void refresh_malformedCookie_returns401() throws Exception {
                        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", "abc:def")))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.message").value("Invalid refresh token."))
                                        .andExpect(cookie().maxAge("refresh_token", 0));
                }
        }

        // ============================================================
        // LOGOUT
        // ============================================================
        @Nested
        class LogoutTests {

                @Test
                void logout_revokesFamilyAndClearsCookies() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());
                        Cookie refreshCookie = cookieOf(registered, "refresh_token");

                        // Act & Assert
                        mockMvc.perform(post("/auth/logout")
                                        .cookie(cookieOf(registered, "access_token"), refreshCookie))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Logged out successfully"))
                                        .andExpect(cookie().maxAge("access_token", 0))
                                        .andExpect(cookie().maxAge("refresh_token", 0));

                        // Assert - session revoked in DB
                        assertTrue(refreshTokenRepository.findById(refreshTokenId(refreshCookie))
                                        .orElseThrow().isRevoked());
                }

                @Test
                void logout_withoutCookies_returns200() throws Exception {
                        // Act & Assert - logout is idempotent
                        mockMvc.perform(post("/auth/logout"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Logged out successfully"))
                                        .andExpect(cookie().maxAge("access_token", 0));
                }
        }

        // ============================================================
        // LOGOUT ALL DEVICES
        // ============================================================
        @Nested
        class LogoutAllTests {

                @Test
                void logoutAll_revokesAllSessions() throws Exception {
                        // Arrange - two sessions: register + login
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        MvcResult login = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        assertEquals(200, login.getResponse().getStatus());
                        String userId = userIdFromBody(registered);
                        assertEquals(2, refreshTokenRepository.findByUserIdAndRevokedFalse(userId).size());

                        // Act & Assert
                        mockMvc.perform(post("/auth/logout-all").cookie(cookieOf(login, "access_token")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Logged out from all devices"));

                        // Assert - every session revoked
                        assertTrue(refreshTokenRepository.findByUserIdAndRevokedFalse(userId).isEmpty());
                }

                @Test
                void logoutAll_withoutAccessToken_fallsBackToSingleLogout() throws Exception {
                        // Arrange - two sessions; only session-1's refresh cookie is sent
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        MvcResult login = loginAs(email, DEFAULT_PASSWORD, uniqueIp());
                        assertEquals(200, login.getResponse().getStatus());
                        String userId = userIdFromBody(registered);

                        // Act - no access_token cookie -> controller falls back to single logout
                        mockMvc.perform(post("/auth/logout-all").cookie(cookieOf(registered, "refresh_token")))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Logged out from all devices"));

                        // Assert - only session-1's family was revoked, the login session survives
                        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
                        assertEquals(1, active.size());
                        assertEquals(refreshTokenId(cookieOf(login, "refresh_token")),
                                        active.get(0).getId());
                }
        }

        // ============================================================
        // CHANGE PASSWORD
        // ============================================================
        @Nested
        class ChangePasswordTests {

                @Test
                void changePassword_success_revokesSessionsAndOldPasswordRejected() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        String userId = userIdFromBody(registered);
                        Cookie accessCookie = cookieOf(registered, "access_token");

                        // Act
                        MvcResult result = performChangePassword(accessCookie,
                                        DEFAULT_PASSWORD, "NewPass1x", "NewPass1x");

                        // Assert
                        assertEquals(200, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Password changed successfully."));
                        assertTrue(refreshTokenRepository.findByUserIdAndRevokedFalse(userId).isEmpty(),
                                        "all sessions must be revoked after password change");

                        assertEquals(401, loginAs(email, DEFAULT_PASSWORD, uniqueIp()).getResponse().getStatus(),
                                        "old password must be rejected");
                        assertEquals(200, loginAs(email, "NewPass1x", uniqueIp()).getResponse().getStatus(),
                                        "new password must work");
                }

                @Test
                void changePassword_wrongCurrentPassword_returns400() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());

                        // Act
                        MvcResult result = performChangePassword(cookieOf(registered, "access_token"),
                                        "WrongCurrent1", "NewPass1x", "NewPass1x");

                        // Assert
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Current password is incorrect."));
                }

                @Test
                void changePassword_withoutCookie_returns401() throws Exception {
                        // Act - body must be VALID (@Valid runs before the controller's auth check)
                        MvcResult result = performChangePassword(null,
                                        DEFAULT_PASSWORD, "NewPass1x", "NewPass1x");

                        // Assert
                        assertEquals(401, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString().contains("Authentication required."));
                }

                @Test
                void changePassword_newPasswordWeak_returns400() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());

                        // Act - passes @Size(8..128) but fails the service-level uppercase/number policy
                        MvcResult result = performChangePassword(cookieOf(registered, "access_token"),
                                        DEFAULT_PASSWORD, "newpassword1", "newpassword1");

                        // Assert
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Password must contain at least 1 uppercase letter and 1 number."));
                }

                @Test
                void changePassword_confirmMismatch_returns400() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());

                        // Act
                        MvcResult result = performChangePassword(cookieOf(registered, "access_token"),
                                        DEFAULT_PASSWORD, "NewPass1x", "DifferentPass1");

                        // Assert
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("New password and confirm password do not match."));
                }

                @Test
                void changePassword_sameAsOld_returns400() throws Exception {
                        // Arrange
                        MvcResult registered = registerUser(uniqueEmail(), uniquePhone());

                        // Act
                        MvcResult result = performChangePassword(cookieOf(registered, "access_token"),
                                        DEFAULT_PASSWORD, DEFAULT_PASSWORD, DEFAULT_PASSWORD);

                        // Assert
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("New password must be different from current password."));
                }
        }

        // ============================================================
        // PASSWORD RESET (forgot password)
        // ============================================================
        @Nested
        class PasswordResetTests {

                @Test
                void passwordResetRequest_existingEmail_sendsOtp() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());

                        // Act
                        MvcResult result = performPasswordResetRequest(email);

                        // Assert - generic response, OTP mailed, hashed OTP persisted
                        assertEquals(200, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("If the email exists, a reset token has been generated."));

                        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
                        verify(emailService).sendPasswordResetEmail(eq(email), otpCaptor.capture());
                        String otp = otpCaptor.getValue();
                        assertTrue(otp.matches("[A-Z0-9]{8}"), "OTP must be 8 uppercase alphanumerics");

                        User user = userRepository.findUserByEmail(email);
                        assertNotNull(user.getPasswordResetToken());
                        assertFalse(user.isPasswordResetUsed());
                        assertTrue(passwordEncoder.matches(otp, user.getPasswordResetToken()),
                                        "OTP must be stored hashed");
                }

                @Test
                void passwordResetRequest_unknownEmail_returns200NoEmail() throws Exception {
                        // Act - same generic 200, but nothing is sent (no user enumeration)
                        MvcResult result = performPasswordResetRequest(uniqueEmail());

                        // Assert
                        assertEquals(200, result.getResponse().getStatus());
                        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
                }

                @Test
                void passwordResetRequest_rateLimited_after3_noFourthEmail() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());

                        // Act - 4 requests; the per-email bucket holds 3
                        for (int i = 0; i < 4; i++) {
                                assertEquals(200, performPasswordResetRequest(email).getResponse().getStatus(),
                                                "rate limiting is silent by design - always 200");
                        }

                        // Assert - only 3 emails actually sent
                        verify(emailService, times(3)).sendPasswordResetEmail(eq(email), anyString());
                }

                @Test
                void passwordResetConfirm_success_thenLoginWithNewPassword() throws Exception {
                        // Arrange - full OTP round-trip via the mocked email service
                        String email = uniqueEmail();
                        MvcResult registered = registerUser(email, uniquePhone());
                        String userId = userIdFromBody(registered);
                        performPasswordResetRequest(email);
                        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
                        verify(emailService).sendPasswordResetEmail(eq(email), otpCaptor.capture());
                        String otp = otpCaptor.getValue();

                        // Act
                        MvcResult result = performPasswordResetConfirm(otp, "ResetPass1");

                        // Assert
                        assertEquals(200, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Password reset successful. You can now log in."));

                        User user = userRepository.findUserByEmail(email);
                        assertNull(user.getPasswordResetToken());
                        assertTrue(user.isPasswordResetUsed());
                        assertTrue(refreshTokenRepository.findByUserIdAndRevokedFalse(userId).isEmpty(),
                                        "all sessions must be revoked after password reset");

                        assertEquals(200, loginAs(email, "ResetPass1", uniqueIp()).getResponse().getStatus());
                        assertEquals(401, loginAs(email, DEFAULT_PASSWORD, uniqueIp()).getResponse().getStatus());
                }

                @Test
                void passwordResetConfirm_wrongOtp_returns400() throws Exception {
                        // Arrange
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());
                        performPasswordResetRequest(email);

                        // Act
                        MvcResult result = performPasswordResetConfirm("WRONGOTP", "ResetPass1");

                        // Assert
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Invalid or expired reset token."));
                }

                @Test
                void passwordResetConfirm_reusedOtp_returns400() throws Exception {
                        // Arrange - confirm once successfully
                        String email = uniqueEmail();
                        registerUser(email, uniquePhone());
                        performPasswordResetRequest(email);
                        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
                        verify(emailService).sendPasswordResetEmail(eq(email), otpCaptor.capture());
                        String otp = otpCaptor.getValue();
                        assertEquals(200, performPasswordResetConfirm(otp, "ResetPass1")
                                        .getResponse().getStatus());

                        // Act - replay the same OTP
                        MvcResult result = performPasswordResetConfirm(otp, "AnotherPass1");

                        // Assert - single-use token
                        assertEquals(400, result.getResponse().getStatus());
                        assertTrue(result.getResponse().getContentAsString()
                                        .contains("Invalid or expired reset token."));
                }
        }
}
