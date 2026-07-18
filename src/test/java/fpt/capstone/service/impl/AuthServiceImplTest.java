package fpt.capstone.service.impl;

import fpt.capstone.dto.request.ChangePasswordRequest;
import fpt.capstone.dto.request.LoginRequest;
import fpt.capstone.dto.request.PasswordResetConfirmRequest;
import fpt.capstone.dto.request.PasswordResetRequest;
import fpt.capstone.dto.request.RegisterRequest;
import fpt.capstone.dto.response.LoginResponse;
import fpt.capstone.entity.RefreshToken;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.User;
import fpt.capstone.enums.AccountStatus;
import fpt.capstone.repository.RefreshTokenRepository;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.AccountLockService;
import fpt.capstone.service.EmailService;
import fpt.capstone.service.PasswordChangeRateLimiterService;
import fpt.capstone.service.PasswordResetRateLimiterService;
import fpt.capstone.service.RateLimiterService;
import fpt.capstone.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private RefreshTokenRepository refreshTokenRepository;
        @Mock
        private RoleRepository roleRepository;
        @Mock
        private JwtUtil jwtUtil;
        @Mock
        private RateLimiterService rateLimiterService;
        @Mock
        private AccountLockService accountLockService;
        @Mock
        private PasswordChangeRateLimiterService passwordChangeRateLimiterService;
        @Mock
        private PasswordResetRateLimiterService passwordResetRateLimiterService;
        @Mock
        private EmailService emailService;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private HttpServletRequest httpRequest;
        @Mock
        private HttpServletResponse httpResponse;

        private AuthServiceImpl authService;

        @Captor
        private ArgumentCaptor<User> userCaptor;
        @Captor
        private ArgumentCaptor<RefreshToken> refreshTokenCaptor;
        @Captor
        private ArgumentCaptor<Cookie> cookieCaptor;

        private User testUser;
        private Role testRole;
        private final String testUserId = "test-user-id";
        private final String testEmail = "test@example.com";
        private final String testPassword = "Password1";
        private final String testEncodedPassword = "$2a$10$encoded";

        @BeforeEach
        void setUp() {
                // Manually construct AuthServiceImpl since it has no default constructor
                authService = new AuthServiceImpl(
                                userRepository,
                                refreshTokenRepository,
                                roleRepository,
                                jwtUtil,
                                rateLimiterService,
                                accountLockService,
                                passwordChangeRateLimiterService,
                                passwordResetRateLimiterService,
                                emailService,
                                passwordEncoder,
                                10 // resetTokenExpiryMinutes
                );

                testRole = new Role();
                testRole.setId(2);
                testRole.setName("Citizen");

                testUser = new User();
                testUser.setId(testUserId);
                testUser.setName("Test User");
                testUser.setEmail(testEmail);
                testUser.setPhone("0123456789");
                testUser.setPassword(testEncodedPassword);
                testUser.setRole(testRole);
                testUser.setDob(LocalDate.of(2000, 1, 1));
                testUser.setFailedLoginAttempts(0);
                testUser.setLockedUntil(null);
                testUser.setPasswordResetUsed(false);
        }

        // ============================================================
        // REGISTER TESTS
        // ============================================================
        @Nested
        class RegisterTests {

                private RegisterRequest validRequest;

                @BeforeEach
                void setUp() {
                        validRequest = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password(testPassword)
                                        .passwordConfirmation(testPassword)
                                        .dateOfBirth("01/01/2000")
                                        .build();
                }

                @Test
                void register_success() {
                        // Arrange
                        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
                        when(userRepository.existsByPhone("0123456789")).thenReturn(false);
                        when(passwordEncoder.encode(testPassword)).thenReturn(testEncodedPassword);
                        when(roleRepository.findByName("Citizen")).thenReturn(Optional.of(testRole));
                        when(jwtUtil.generateAccessToken(isNull(), eq(testEmail), eq("Citizen")))
                                        .thenReturn("access-token");
                        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
                        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);

                        // Act
                        LoginResponse response = authService.register(validRequest, httpRequest, httpResponse);

                        // Assert
                        assertNotNull(response);
                        assertEquals(testEmail, response.getEmail());
                        assertEquals("Test User", response.getName());
                        assertEquals("Citizen", response.getRole());

                        verify(userRepository).save(userCaptor.capture());
                        User savedUser = userCaptor.getValue();
                        assertEquals(testEmail, savedUser.getEmail());
                        assertEquals("0123456789", savedUser.getPhone());
                        assertEquals(testEncodedPassword, savedUser.getPassword());
                        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
                        assertEquals(Boolean.FALSE, savedUser.getNationalIdVerified());

                        verify(refreshTokenRepository).save(any(RefreshToken.class));
                        verify(httpResponse, times(2)).addCookie(any(Cookie.class));
                }

                @Test
                void register_passwordConfirmationMismatch() {
                        // Arrange
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password(testPassword)
                                        .passwordConfirmation("DifferentPassword1")
                                        .dateOfBirth("01/01/2000")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("do not match"));
                        verify(userRepository, never()).save(any());
                }

                @Test
                void register_passwordTooShort() {
                        // Arrange
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password("Ab1")
                                        .passwordConfirmation("Ab1")
                                        .dateOfBirth("01/01/2000")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Password must be between"));
                }

                @Test
                void register_passwordTooLong() {
                        // Arrange
                        String longPassword = "A1" + "a".repeat(130);
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password(longPassword)
                                        .passwordConfirmation(longPassword)
                                        .dateOfBirth("01/01/2000")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                }

                @Test
                void register_passwordMissingUppercase() {
                        // Arrange
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password("password1")
                                        .passwordConfirmation("password1")
                                        .dateOfBirth("01/01/2000")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("uppercase"));
                }

                @Test
                void register_passwordMissingNumber() {
                        // Arrange
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password("Password")
                                        .passwordConfirmation("Password")
                                        .dateOfBirth("01/01/2000")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("number"));
                }

                @Test
                void register_passwordNull() {
                        // Arrange
                        // When password is null, the code throws NPE at
                        // password.equals(passwordConfirmation)
                        // which is a bug in the code - but we test the actual behavior
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password(null)
                                        .passwordConfirmation(null)
                                        .dateOfBirth("01/01/2000")
                                        .build();

                        // Act & Assert - NPE is thrown before the validatePassword check
                        assertThrows(NullPointerException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        verify(userRepository, never()).save(any());
                }

                @Test
                void register_invalidDateFormat() {
                        // Arrange
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password(testPassword)
                                        .passwordConfirmation(testPassword)
                                        .dateOfBirth("2000/01/01")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Invalid date of birth format"));
                }

                @Test
                void register_dateOfBirthBefore1900() {
                        // Arrange
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password(testPassword)
                                        .passwordConfirmation(testPassword)
                                        .dateOfBirth("31/12/1899")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("before 01/01/1900"));
                }

                @Test
                void register_dateOfBirthInFuture() {
                        // Arrange
                        RegisterRequest request = RegisterRequest.builder()
                                        .fullName("Test User")
                                        .email(testEmail)
                                        .phone("0123456789")
                                        .password(testPassword)
                                        .passwordConfirmation(testPassword)
                                        .dateOfBirth("01/01/2100")
                                        .build();

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(request, httpRequest, httpResponse));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("future"));
                }

                @Test
                void register_emailAlreadyExists() {
                        // Arrange
                        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(validRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Email already exists"));
                }

                @Test
                void register_phoneAlreadyExists() {
                        // Arrange
                        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
                        when(userRepository.existsByPhone("0123456789")).thenReturn(true);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(validRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Phone number already exists"));
                }

                @Test
                void register_databaseException() {
                        // Arrange
                        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
                        when(userRepository.existsByPhone("0123456789")).thenReturn(false);
                        when(passwordEncoder.encode(testPassword)).thenReturn(testEncodedPassword);
                        when(roleRepository.findByName("Citizen")).thenReturn(Optional.of(testRole));
                        doThrow(new DataAccessException("DB error") {
                        }).when(userRepository).save(any(User.class));

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(validRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Service temporarily unavailable"));
                }

                @Test
                void register_citizenRoleMissing_throwsInternalServerError() {
                        // Arrange
                        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
                        when(userRepository.existsByPhone("0123456789")).thenReturn(false);
                        when(passwordEncoder.encode(testPassword)).thenReturn(testEncodedPassword);
                        when(roleRepository.findByName("Citizen")).thenReturn(Optional.empty());

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.register(validRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Default Citizen role"));
                        verify(userRepository, never()).save(any());
                }
        }

        // ============================================================
        // LOGIN TESTS
        // ============================================================
        @Nested
        class LoginTests {

                private LoginRequest loginRequest;
                private final String testIp = "192.168.1.1";

                @BeforeEach
                void setUp() {
                        loginRequest = new LoginRequest();
                        loginRequest.setCredential(testEmail);
                        loginRequest.setPassword(testPassword);

                        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                        lenient().when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
                        lenient().when(httpRequest.getRemoteAddr()).thenReturn(testIp);
                }

                @Test
                void login_successByEmail() {
                        // Arrange
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
                        when(passwordEncoder.matches(testPassword, testEncodedPassword)).thenReturn(true);
                        when(jwtUtil.generateAccessToken(testUserId, testEmail, "Citizen")).thenReturn("access-token");
                        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
                        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);

                        // Act
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);

                        // Assert
                        assertNotNull(response);
                        assertEquals(testEmail, response.getEmail());
                        assertEquals("Test User", response.getName());
                        verify(accountLockService).resetFailedAttempts(testUser);
                        verify(refreshTokenRepository).save(any(RefreshToken.class));
                }

                @Test
                void login_successByPhone() {
                        // Arrange
                        loginRequest.setCredential("0123456789");
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByPhone("0123456789")).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
                        when(passwordEncoder.matches(testPassword, testEncodedPassword)).thenReturn(true);
                        when(jwtUtil.generateAccessToken(testUserId, testEmail, "Citizen")).thenReturn("access-token");
                        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
                        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);

                        // Act
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);

                        // Assert
                        assertNotNull(response);
                        assertEquals(testEmail, response.getEmail());
                        verify(accountLockService).resetFailedAttempts(testUser);
                }

                @Test
                void login_twelveDigitCredential_looksUpByPhone() {
                        // Arrange - national ID login was removed: a 12-digit credential
                        // is treated as a phone number lookup
                        loginRequest.setCredential("123456789012");
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByPhone("123456789012")).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
                        when(passwordEncoder.matches(testPassword, testEncodedPassword)).thenReturn(true);
                        when(jwtUtil.generateAccessToken(testUserId, testEmail, "Citizen")).thenReturn("access-token");
                        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
                        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);

                        // Act
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);

                        // Assert
                        assertNotNull(response);
                        assertEquals(testEmail, response.getEmail());
                        verify(userRepository).findUserByPhone("123456789012");
                        verify(accountLockService).resetFailedAttempts(testUser);
                }

                @Test
                void login_userNotFound() {
                        // Arrange
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(null);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.login(loginRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Incorrect username or password"));
                }

                @Test
                void login_bannedAccount_throwsForbidden() {
                        // Arrange
                        testUser.setStatus(AccountStatus.BANNED);
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(testUser);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.login(loginRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
                        assertEquals("ACCOUNT_BANNED", ex.getReason());
                        // Ban outranks the temporary lock and no token is issued
                        verify(accountLockService, never()).isAccountLocked(any());
                        verify(refreshTokenRepository, never()).save(any());
                        verify(httpResponse, never()).addCookie(any());
                }

                @Test
                void login_activeStatus_notBanned_succeeds() {
                        // Arrange - explicit ACTIVE covers the false side of the BANNED branch
                        testUser.setStatus(AccountStatus.ACTIVE);
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
                        when(passwordEncoder.matches(testPassword, testEncodedPassword)).thenReturn(true);
                        when(jwtUtil.generateAccessToken(testUserId, testEmail, "Citizen")).thenReturn("access-token");
                        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
                        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);

                        // Act
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);

                        // Assert
                        assertNotNull(response);
                        verify(accountLockService).resetFailedAttempts(testUser);
                }

                @Test
                void login_accountLocked() {
                        // Arrange
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(true);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.login(loginRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("account is temporarily locked"));
                }

                @Test
                void login_wrongPassword() {
                        // Arrange
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
                        when(passwordEncoder.matches(testPassword, testEncodedPassword)).thenReturn(false);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.login(loginRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Incorrect username or password"));
                        verify(accountLockService).recordFailedAttempt(testUser);
                }

                @Test
                void login_rateLimitExceeded() {
                        // Arrange
                        when(rateLimiterService.tryConsume(testIp))
                                        .thenReturn(new RateLimiterService.RateLimitResult(false, 60));

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.login(loginRequest, httpRequest, httpResponse));
                        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
                        verify(httpResponse).setHeader("Retry-After", "60");
                        verify(userRepository, never()).findUserByEmail(anyString());
                }
        }

        // ============================================================
        // REFRESH ACCESS TOKEN TESTS
        // ============================================================
        @Nested
        class RefreshTokenTests {

                private final String refreshCookieValue = "1:raw-refresh-token";
                private RefreshToken storedToken;

                @BeforeEach
                void setUp() {
                        storedToken = RefreshToken.builder()
                                        .id(1L)
                                        .token("encoded-refresh-token")
                                        .userId(testUserId)
                                        .familyId("family-1")
                                        .expiresAt(Instant.now().plusSeconds(86400))
                                        .createdAt(Instant.now())
                                        .revoked(false)
                                        .build();

                        Cookie[] cookies = new Cookie[] { new Cookie("refresh_token", refreshCookieValue) };
                        lenient().when(httpRequest.getCookies()).thenReturn(cookies);
                        lenient().when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
                        lenient().when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
                }

                @Test
                void refreshAccessToken_success() {
                        // Arrange
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));
                        when(passwordEncoder.matches("raw-refresh-token", "encoded-refresh-token")).thenReturn(true);
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
                        when(jwtUtil.generateAccessToken(testUserId, testEmail, "Citizen")).thenReturn("new-access-token");

                        // Act
                        LoginResponse response = authService.refreshAccessToken(httpRequest, httpResponse);

                        // Assert
                        assertNotNull(response);
                        assertEquals(testEmail, response.getEmail());
                        verify(refreshTokenRepository).save(storedToken); // revoked old token
                        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // old + new
                        verify(httpResponse, times(2)).addCookie(any(Cookie.class));
                }

                @Test
                void refreshAccessToken_cookieNull() {
                        // Arrange
                        when(httpRequest.getCookies()).thenReturn(null);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("No valid refresh token"));
                }

                @Test
                void refreshAccessToken_refreshCookieAbsent_throwsUnauthorized() {
                        // Arrange - cookie array present but contains no refresh_token cookie
                        Cookie[] cookies = new Cookie[] { new Cookie("access_token", "some-access-token") };
                        when(httpRequest.getCookies()).thenReturn(cookies);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("No valid refresh token"));
                }

                @Test
                void refreshAccessToken_noColonInCookie() {
                        // Arrange
                        Cookie[] cookies = new Cookie[] { new Cookie("refresh_token", "invalid-format") };
                        when(httpRequest.getCookies()).thenReturn(cookies);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                }

                @Test
                void refreshAccessToken_invalidTokenId() {
                        // Arrange
                        Cookie[] cookies = new Cookie[] { new Cookie("refresh_token", "abc:raw-token") };
                        when(httpRequest.getCookies()).thenReturn(cookies);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void refreshAccessToken_tokenNotFound() {
                        // Arrange
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.empty());

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void refreshAccessToken_expired() {
                        // Arrange
                        storedToken.setExpiresAt(Instant.now().minusSeconds(3600));
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("expired"));
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void refreshAccessToken_revokedToken() {
                        // Arrange
                        storedToken.setRevoked(true);
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));

                        // Act & Assert
                        // The code checks isRevoked() before matching the password, so
                        // passwordEncoder.matches is not called
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Token reuse detected"));
                        verify(refreshTokenRepository).revokeFamily("family-1");
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void refreshAccessToken_tokenHashMismatch() {
                        // Arrange
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));
                        when(passwordEncoder.matches("raw-refresh-token", "encoded-refresh-token")).thenReturn(false);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Invalid token"));
                        verify(refreshTokenRepository).revokeFamily("family-1");
                }

                @Test
                void refreshAccessToken_userNotFound() {
                        // Arrange
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));
                        when(passwordEncoder.matches("raw-refresh-token", "encoded-refresh-token")).thenReturn(true);
                        when(userRepository.getUserById(testUserId)).thenReturn(null);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("User not found"));
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void refreshAccessToken_accountLocked() {
                        // Arrange
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(storedToken));
                        when(passwordEncoder.matches("raw-refresh-token", "encoded-refresh-token")).thenReturn(true);
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(accountLockService.isAccountLocked(testUser)).thenReturn(true);
                        when(accountLockService.getLockRemainingSeconds(testUser)).thenReturn(180L);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.refreshAccessToken(httpRequest, httpResponse));
                        assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Account is locked"));
                        verify(refreshTokenRepository).revokeFamily("family-1");
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }
        }

        // ============================================================
        // LOGOUT TESTS
        // ============================================================
        @Nested
        class LogoutTests {

                @Test
                void logout_withValidCookie() {
                        // Arrange
                        Cookie[] cookies = new Cookie[] { new Cookie("refresh_token", "1:token-value") };
                        when(httpRequest.getCookies()).thenReturn(cookies);
                        RefreshToken token = RefreshToken.builder().familyId("family-1").build();
                        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(token));

                        // Act
                        authService.logout(httpRequest, httpResponse);

                        // Assert
                        verify(refreshTokenRepository).revokeFamily("family-1");
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void logout_withInvalidCookie() {
                        // Arrange
                        when(httpRequest.getCookies()).thenReturn(null);

                        // Act
                        authService.logout(httpRequest, httpResponse);

                        // Assert
                        verify(refreshTokenRepository, never()).revokeFamily(anyString());
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void logout_withoutColon() {
                        // Arrange
                        Cookie[] cookies = new Cookie[] { new Cookie("refresh_token", "no-colon") };
                        when(httpRequest.getCookies()).thenReturn(cookies);

                        // Act
                        authService.logout(httpRequest, httpResponse);

                        // Assert
                        verify(refreshTokenRepository, never()).revokeFamily(anyString());
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void logout_tokenIdNotFound_stillClearsCookies() {
                        // Arrange
                        Cookie[] cookies = new Cookie[] { new Cookie("refresh_token", "99:token-value") };
                        when(httpRequest.getCookies()).thenReturn(cookies);
                        when(refreshTokenRepository.findById(99L)).thenReturn(Optional.empty());

                        // Act
                        authService.logout(httpRequest, httpResponse);

                        // Assert - familyId resolves to null but cookies are still cleared
                        verify(refreshTokenRepository).revokeFamily(isNull());
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }

                @Test
                void logout_exceptionDuringRevoke() {
                        // Arrange
                        Cookie[] cookies = new Cookie[] { new Cookie("refresh_token", "1:token-value") };
                        when(httpRequest.getCookies()).thenReturn(cookies);
                        when(refreshTokenRepository.findById(1L)).thenThrow(new RuntimeException("Error"));

                        // Act - should not throw
                        authService.logout(httpRequest, httpResponse);

                        // Assert
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }
        }

        // ============================================================
        // LOGOUT ALL DEVICES TESTS
        // ============================================================
        @Nested
        class LogoutAllDevicesTests {

                @Test
                void logoutAllDevices_success() {
                        // Act
                        authService.logoutAllDevices(testUserId, httpResponse);

                        // Assert
                        verify(refreshTokenRepository).revokeAllByUserId(testUserId);
                        verify(httpResponse, atLeast(1)).addCookie(any(Cookie.class));
                }
        }

        // ============================================================
        // REQUEST PASSWORD RESET TESTS
        // ============================================================
        @Nested
        class RequestPasswordResetTests {

                private PasswordResetRequest request;

                @BeforeEach
                void setUp() {
                        request = new PasswordResetRequest();
                        request.setEmail(testEmail);
                }

                @Test
                void requestPasswordReset_success() {
                        // Arrange
                        when(passwordResetRateLimiterService.tryConsume(testEmail.toLowerCase()))
                                        .thenReturn(new PasswordResetRateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(testUser);
                        when(passwordEncoder.encode(anyString())).thenReturn("encoded-otp");

                        // Act
                        authService.requestPasswordReset(request);

                        // Assert
                        verify(userRepository).save(userCaptor.capture());
                        User savedUser = userCaptor.getValue();
                        assertNotNull(savedUser.getPasswordResetToken());
                        assertNotNull(savedUser.getPasswordResetTokenExpiry());
                        assertFalse(savedUser.isPasswordResetUsed());
                        verify(emailService).sendPasswordResetEmail(eq(testEmail.toLowerCase()), anyString());
                }

                @Test
                void requestPasswordReset_emailNotFound() {
                        // Arrange
                        when(passwordResetRateLimiterService.tryConsume(testEmail.toLowerCase()))
                                        .thenReturn(new PasswordResetRateLimiterService.RateLimitResult(true, 0));
                        when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(null);

                        // Act - should not throw
                        authService.requestPasswordReset(request);

                        // Assert
                        verify(userRepository, never()).save(any());
                        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
                }

                @Test
                void requestPasswordReset_rateLimited() {
                        // Arrange
                        when(passwordResetRateLimiterService.tryConsume(testEmail.toLowerCase()))
                                        .thenReturn(new PasswordResetRateLimiterService.RateLimitResult(false, 120));

                        // Act - should not throw (silent return)
                        authService.requestPasswordReset(request);

                        // Assert
                        verify(userRepository, never()).findUserByEmail(anyString());
                        verify(userRepository, never()).save(any());
                        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
                }
        }

        // ============================================================
        // CONFIRM PASSWORD RESET TESTS
        // ============================================================
        @Nested
        class ConfirmPasswordResetTests {

                private PasswordResetConfirmRequest request;
                private User userWithResetToken;

                @BeforeEach
                void setUp() {
                        request = new PasswordResetConfirmRequest();
                        request.setResetToken("VALID-OTP-TOKEN");
                        request.setNewPassword("NewPassword1");

                        userWithResetToken = new User();
                        userWithResetToken.setId(testUserId);
                        userWithResetToken.setEmail(testEmail);
                        userWithResetToken.setPassword(testEncodedPassword);
                        userWithResetToken.setPasswordResetToken("encoded-otp");
                        userWithResetToken.setPasswordResetTokenExpiry(Instant.now().plusSeconds(3600));
                        userWithResetToken.setPasswordResetUsed(false);
                        userWithResetToken.setFailedLoginAttempts(3);
                        userWithResetToken.setLockedUntil(Instant.now().plusSeconds(3600));
                }

                @Test
                void confirmPasswordReset_success() {
                        // Arrange
                        when(userRepository
                                        .findByPasswordResetTokenIsNotNullAndPasswordResetUsedFalseAndPasswordResetTokenExpiryAfter(
                                                        any(Instant.class)))
                                        .thenReturn(List.of(userWithResetToken));
                        when(passwordEncoder.matches("VALID-OTP-TOKEN", "encoded-otp")).thenReturn(true);
                        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-encoded-password");

                        // Act
                        authService.confirmPasswordReset(request);

                        // Assert
                        verify(userRepository).save(userCaptor.capture());
                        User savedUser = userCaptor.getValue();
                        assertTrue(savedUser.isPasswordResetUsed());
                        assertNull(savedUser.getPasswordResetToken());
                        assertNull(savedUser.getPasswordResetTokenExpiry());
                        assertEquals("new-encoded-password", savedUser.getPassword());
                        verify(accountLockService).unlockAccount(userWithResetToken);
                        verify(refreshTokenRepository).revokeAllByUserId(testUserId);
                }

                @Test
                void confirmPasswordReset_tokenNull() {
                        // Arrange
                        request.setResetToken(null);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.confirmPasswordReset(request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Reset token is required"));
                }

                @Test
                void confirmPasswordReset_tokenBlank() {
                        // Arrange
                        request.setResetToken("   ");

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.confirmPasswordReset(request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                }

                @Test
                void confirmPasswordReset_invalidToken() {
                        // Arrange
                        when(userRepository
                                        .findByPasswordResetTokenIsNotNullAndPasswordResetUsedFalseAndPasswordResetTokenExpiryAfter(
                                                        any(Instant.class)))
                                        .thenReturn(List.of(userWithResetToken));
                        when(passwordEncoder.matches("VALID-OTP-TOKEN", "encoded-otp")).thenReturn(false);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.confirmPasswordReset(request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Invalid or expired reset token"));
                }

                @Test
                void confirmPasswordReset_noCandidates() {
                        // Arrange
                        when(userRepository
                                        .findByPasswordResetTokenIsNotNullAndPasswordResetUsedFalseAndPasswordResetTokenExpiryAfter(
                                                        any(Instant.class)))
                                        .thenReturn(List.of());

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.confirmPasswordReset(request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                }

                @Test
                void confirmPasswordReset_invalidNewPassword() {
                        // Arrange
                        request.setNewPassword("short");

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.confirmPasswordReset(request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Password must be between"));
                }

                @Test
                void confirmPasswordReset_newPasswordNull_throwsBadRequest() {
                        // Arrange
                        request.setNewPassword(null);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.confirmPasswordReset(request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Password must be between"));
                        verify(userRepository, never()).save(any());
                }

                @Test
                void confirmPasswordReset_secondCandidateMatches() {
                        // Arrange - two candidates; the OTP only matches the second one
                        User otherCandidate = new User();
                        otherCandidate.setId("other-user-id");
                        otherCandidate.setEmail("other@example.com");
                        otherCandidate.setPasswordResetToken("other-encoded-otp");
                        when(userRepository
                                        .findByPasswordResetTokenIsNotNullAndPasswordResetUsedFalseAndPasswordResetTokenExpiryAfter(
                                                        any(Instant.class)))
                                        .thenReturn(List.of(otherCandidate, userWithResetToken));
                        when(passwordEncoder.matches("VALID-OTP-TOKEN", "other-encoded-otp")).thenReturn(false);
                        when(passwordEncoder.matches("VALID-OTP-TOKEN", "encoded-otp")).thenReturn(true);
                        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-encoded-password");

                        // Act
                        authService.confirmPasswordReset(request);

                        // Assert - only the matching (second) candidate is updated
                        verify(userRepository).save(userCaptor.capture());
                        assertEquals(testUserId, userCaptor.getValue().getId());
                        verify(accountLockService).unlockAccount(userWithResetToken);
                        verify(accountLockService, never()).unlockAccount(otherCandidate);
                        verify(refreshTokenRepository).revokeAllByUserId(testUserId);
                }
        }

        // ============================================================
        // CHANGE PASSWORD TESTS
        // ============================================================
        @Nested
        class ChangePasswordTests {

                private ChangePasswordRequest request;

                @BeforeEach
                void setUp() {
                        request = new ChangePasswordRequest();
                        request.setCurrentPassword("OldPassword1");
                        request.setNewPassword("NewPassword1");
                        request.setConfirmNewPassword("NewPassword1");
                }

                @Test
                void changePassword_success() {
                        // Arrange
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(passwordChangeRateLimiterService.tryConsume(testUserId))
                                        .thenReturn(new PasswordChangeRateLimiterService.RateLimitResult(true, 0));
                        when(passwordEncoder.matches("OldPassword1", testEncodedPassword)).thenReturn(true);
                        when(passwordEncoder.matches("NewPassword1", testEncodedPassword)).thenReturn(false);
                        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-encoded");

                        // Act
                        authService.changePassword(testUserId, request);

                        // Assert
                        verify(userRepository).save(userCaptor.capture());
                        assertEquals("new-encoded", userCaptor.getValue().getPassword());
                        verify(refreshTokenRepository).revokeAllByUserId(testUserId);
                }

                @Test
                void changePassword_userNotFound() {
                        // Arrange
                        when(userRepository.getUserById(testUserId)).thenReturn(null);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.changePassword(testUserId, request));
                        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("User not found"));
                }

                @Test
                void changePassword_rateLimitExceeded() {
                        // Arrange
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(passwordChangeRateLimiterService.tryConsume(testUserId))
                                        .thenReturn(new PasswordChangeRateLimiterService.RateLimitResult(false, 60));

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.changePassword(testUserId, request));
                        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Too many password change attempts"));
                }

                @Test
                void changePassword_currentPasswordIncorrect() {
                        // Arrange
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(passwordChangeRateLimiterService.tryConsume(testUserId))
                                        .thenReturn(new PasswordChangeRateLimiterService.RateLimitResult(true, 0));
                        when(passwordEncoder.matches("OldPassword1", testEncodedPassword)).thenReturn(false);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.changePassword(testUserId, request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Current password is incorrect"));
                }

                @Test
                void changePassword_newPasswordNotMatchConfirm() {
                        // Arrange
                        request.setConfirmNewPassword("DifferentPassword1");
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(passwordChangeRateLimiterService.tryConsume(testUserId))
                                        .thenReturn(new PasswordChangeRateLimiterService.RateLimitResult(true, 0));
                        when(passwordEncoder.matches("OldPassword1", testEncodedPassword)).thenReturn(true);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.changePassword(testUserId, request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("do not match"));
                }

                @Test
                void changePassword_newPasswordSameAsCurrent() {
                        // Arrange
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(passwordChangeRateLimiterService.tryConsume(testUserId))
                                        .thenReturn(new PasswordChangeRateLimiterService.RateLimitResult(true, 0));
                        when(passwordEncoder.matches("OldPassword1", testEncodedPassword)).thenReturn(true);
                        when(passwordEncoder.matches("NewPassword1", testEncodedPassword)).thenReturn(true);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.changePassword(testUserId, request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("different from current password"));
                }

                @Test
                void changePassword_invalidNewPassword() {
                        // Arrange
                        request.setNewPassword("weak");
                        request.setConfirmNewPassword("weak");
                        when(userRepository.getUserById(testUserId)).thenReturn(testUser);
                        when(passwordChangeRateLimiterService.tryConsume(testUserId))
                                        .thenReturn(new PasswordChangeRateLimiterService.RateLimitResult(true, 0));
                        when(passwordEncoder.matches("OldPassword1", testEncodedPassword)).thenReturn(true);

                        // Act & Assert
                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> authService.changePassword(testUserId, request));
                        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                }
        }

        // ============================================================
        // CLIENT IP EXTRACTION TESTS (via login)
        // ============================================================
        @Nested
        class ClientIpTests {

                private LoginRequest loginRequest;

                @BeforeEach
                void setUp() {
                        loginRequest = new LoginRequest();
                        loginRequest.setCredential(testEmail);
                        loginRequest.setPassword(testPassword);

                        lenient().when(userRepository.findUserByEmail(testEmail.toLowerCase())).thenReturn(testUser);
                        lenient().when(accountLockService.isAccountLocked(testUser)).thenReturn(false);
                        lenient().when(passwordEncoder.matches(testPassword, testEncodedPassword)).thenReturn(true);
                        lenient().when(jwtUtil.generateAccessToken(testUserId, testEmail, "Citizen"))
                                        .thenReturn("access-token");
                        lenient().when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
                        lenient().when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
                }

                @Test
                void getClientIp_usesXForwardedFor() {
                        // Arrange
                        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
                        // X-Real-IP defaults to null (mock default)
                        when(rateLimiterService.tryConsume("10.0.0.1"))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));

                        // Act & Assert - login succeeds
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);
                        assertNotNull(response);
                        verify(rateLimiterService).tryConsume("10.0.0.1");
                }

                @Test
                void getClientIp_usesXRealIp() {
                        // Arrange
                        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                        lenient().when(httpRequest.getHeader("X-Real-IP")).thenReturn("10.0.0.5");
                        when(rateLimiterService.tryConsume("10.0.0.5"))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));

                        // Act & Assert
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);
                        assertNotNull(response);
                        verify(rateLimiterService).tryConsume("10.0.0.5");
                }

                @Test
                void getClientIp_usesRemoteAddr() {
                        // Arrange
                        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.100");
                        // X-Forwarded-For and X-Real-IP default to null (mock default)
                        when(rateLimiterService.tryConsume("192.168.1.100"))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));

                        // Act & Assert
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);
                        assertNotNull(response);
                        verify(rateLimiterService).tryConsume("192.168.1.100");
                }

                @Test
                void getClientIp_blankXForwardedFor_usesXRealIp() {
                        // Arrange
                        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("   ");
                        when(httpRequest.getHeader("X-Real-IP")).thenReturn("10.0.0.7");
                        when(rateLimiterService.tryConsume("10.0.0.7"))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));

                        // Act & Assert
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);
                        assertNotNull(response);
                        verify(rateLimiterService).tryConsume("10.0.0.7");
                }

                @Test
                void getClientIp_blankXRealIp_usesRemoteAddr() {
                        // Arrange
                        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                        when(httpRequest.getHeader("X-Real-IP")).thenReturn("  ");
                        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.50");
                        when(rateLimiterService.tryConsume("192.168.1.50"))
                                        .thenReturn(new RateLimiterService.RateLimitResult(true, 0));

                        // Act & Assert
                        LoginResponse response = authService.login(loginRequest, httpRequest, httpResponse);
                        assertNotNull(response);
                        verify(rateLimiterService).tryConsume("192.168.1.50");
                }
        }
}