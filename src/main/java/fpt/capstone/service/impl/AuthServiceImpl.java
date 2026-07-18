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
import fpt.capstone.repository.RefreshTokenRepository;
import fpt.capstone.repository.RoleRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.AccountLockService;
import fpt.capstone.service.AuthService;
import fpt.capstone.service.EmailService;
import fpt.capstone.service.PasswordChangeRateLimiterService;
import fpt.capstone.service.PasswordResetRateLimiterService;
import fpt.capstone.service.RateLimiterService;
import fpt.capstone.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final RateLimiterService rateLimiterService;
    private final AccountLockService accountLockService;
    private final PasswordChangeRateLimiterService passwordChangeRateLimiterService;
    private final PasswordResetRateLimiterService passwordResetRateLimiterService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private final int resetTokenExpiryMinutes;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            RoleRepository roleRepository,
            JwtUtil jwtUtil,
            RateLimiterService rateLimiterService,
            AccountLockService accountLockService,
            PasswordChangeRateLimiterService passwordChangeRateLimiterService,
            PasswordResetRateLimiterService passwordResetRateLimiterService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @org.springframework.beans.factory.annotation.Value("${auth.reset-password.otp.expiry-minutes}") int resetTokenExpiryMinutes) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.rateLimiterService = rateLimiterService;
        this.accountLockService = accountLockService;
        this.passwordChangeRateLimiterService = passwordChangeRateLimiterService;
        this.passwordResetRateLimiterService = passwordResetRateLimiterService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenExpiryMinutes = resetTokenExpiryMinutes;
    }

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).+$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            return doRegister(request, httpRequest, httpResponse);
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("Database error during registration: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service temporarily unavailable. Please try again later.");
        }
    }

    @Transactional
    protected LoginResponse doRegister(RegisterRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String fullName = request.getFullName().trim();
        String email = request.getEmail().toLowerCase().trim();
        String phone = request.getPhone().trim();
        String password = request.getPassword();
        String passwordConfirmation = request.getPasswordConfirmation();

        // Validate password match
        if (!password.equals(passwordConfirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password and password confirmation do not match.");
        }

        validatePassword(password);

        // Validate date of birth
        LocalDate dob;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            dob = LocalDate.parse(request.getDateOfBirth(), formatter);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid date of birth format. Use DD/MM/YYYY.");
        }

        LocalDate minDate = LocalDate.of(1900, 1, 1);
        LocalDate maxDate = LocalDate.now();
        if (dob.isBefore(minDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date of birth cannot be before 01/01/1900.");
        }
        if (dob.isAfter(maxDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date of birth cannot be in the future.");
        }

        // Check duplicate email or phone
        if (userRepository.existsByEmail(email)) {
            log.info("Registration attempt with existing email: {}", email);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already exists.");
        }
        if (userRepository.existsByPhone(phone)) {
            log.info("Registration attempt with existing phone: {}", phone);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phone number already exists.");
        }

        User user = new User();
        user.setName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDob(dob);
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(email);

        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Default Citizen role is not configured. Please contact the administrator."));
        user.setRole(defaultRole);

        userRepository.save(user);

        log.info("User registered successfully: {}", email);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());
        String refreshTokenValue = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(passwordEncoder.encode(refreshTokenValue))
                .userId(user.getId())
                .familyId(familyId)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()))
                .createdAt(Instant.now())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        setCookie(httpResponse, ACCESS_TOKEN_COOKIE, accessToken,
                (int) (jwtUtil.getAccessTokenExpiration() / 1000));
        setCookie(httpResponse, REFRESH_TOKEN_COOKIE, refreshTokenEntity.getId() + ":" + refreshTokenValue,
                (int) (jwtUtil.getRefreshTokenExpiration() / 1000));

        Instant accessTokenExpiry = Instant.now().plusMillis(jwtUtil.getAccessTokenExpiration());
        return LoginResponse.fromUser(user, accessTokenExpiry);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String ip = getClientIp(httpRequest);

        RateLimiterService.RateLimitResult rateLimit = rateLimiterService.tryConsume(ip);
        if (!rateLimit.allowed()) {
            httpResponse.setHeader("Retry-After", String.valueOf(rateLimit.retryAfterSeconds()));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Retry after " + rateLimit.retryAfterSeconds() + " seconds.");
        }
        String credential = request.getCredential().trim();

        // Determine credential type: email or phone
        User user = null;
        if (credential.contains("@")) {
            // Email
            user = userRepository.findUserByEmail(credential.toLowerCase());
        } else {
            // Phone number
            user = userRepository.findUserByPhone(credential);
        }

        if (user == null) {
            // Generic error message (MSG-05) - don't reveal which field is wrong
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Incorrect username or password. Please check again.");
        }

        if (accountLockService.isAccountLocked(user)) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Your account is temporarily locked. Please try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            accountLockService.recordFailedAttempt(user);
            // Generic error message (MSG-05)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Incorrect username or password. Please check again.");
        }

        accountLockService.resetFailedAttempts(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());
        String refreshTokenValue = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(passwordEncoder.encode(refreshTokenValue))
                .userId(user.getId())
                .familyId(familyId)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()))
                .createdAt(Instant.now())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        setCookie(httpResponse, ACCESS_TOKEN_COOKIE, accessToken,
                (int) (jwtUtil.getAccessTokenExpiration() / 1000));
        setCookie(httpResponse, REFRESH_TOKEN_COOKIE, refreshTokenEntity.getId() + ":" + refreshTokenValue,
                (int) (jwtUtil.getRefreshTokenExpiration() / 1000));

        Instant accessTokenExpiry = Instant.now().plusMillis(jwtUtil.getAccessTokenExpiration());
        return LoginResponse.fromUser(user, accessTokenExpiry);
    }

    @Override
    @Transactional
    public LoginResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshCookieValue = getCookieValue(request, REFRESH_TOKEN_COOKIE);
        if (refreshCookieValue == null || !refreshCookieValue.contains(":")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No valid refresh token.");
        }

        String[] parts = refreshCookieValue.split(":", 2);

        long tokenId;
        try {
            tokenId = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            revokeTokenFamilyByCookieValue(refreshCookieValue, response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.");
        }
        String rawToken = parts[1];

        RefreshToken storedToken = refreshTokenRepository.findById(tokenId).orElse(null);
        if (storedToken == null) {
            revokeTokenFamilyByCookieValue(refreshCookieValue, response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            revokeTokenFamilyByCookieValue(refreshCookieValue, response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired.");
        }

        if (storedToken.isRevoked()) {
            refreshTokenRepository.revokeFamily(storedToken.getFamilyId());
            revokeCookies(response);
            log.warn("Reuse of revoked refresh token detected. Revoked entire family: {}", storedToken.getFamilyId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token reuse detected. All sessions revoked.");
        }

        if (!passwordEncoder.matches(rawToken, storedToken.getToken())) {
            refreshTokenRepository.revokeFamily(storedToken.getFamilyId());
            revokeCookies(response);
            log.warn("Invalid refresh token hash detected. Revoked entire family: {}", storedToken.getFamilyId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid token. All sessions revoked.");
        }

        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        User user = userRepository.getUserById(storedToken.getUserId());
        if (user == null) {
            revokeCookies(response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }

        if (accountLockService.isAccountLocked(user)) {
            refreshTokenRepository.revokeFamily(storedToken.getFamilyId());
            revokeCookies(response);
            long remaining = accountLockService.getLockRemainingSeconds(user);
            long remainingMinutes = (long) Math.ceil(remaining / 60.0);
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Account is locked. Try again in " + remainingMinutes + " minute(s).");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());
        String newRefreshTokenValue = UUID.randomUUID().toString();

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(passwordEncoder.encode(newRefreshTokenValue))
                .userId(user.getId())
                .familyId(storedToken.getFamilyId())
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()))
                .createdAt(Instant.now())
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        setCookie(response, ACCESS_TOKEN_COOKIE, newAccessToken,
                (int) (jwtUtil.getAccessTokenExpiration() / 1000));
        setCookie(response, REFRESH_TOKEN_COOKIE,
                newRefreshTokenEntity.getId() + ":" + newRefreshTokenValue,
                (int) (jwtUtil.getRefreshTokenExpiration() / 1000));

        Instant accessTokenExpiry = Instant.now().plusMillis(jwtUtil.getAccessTokenExpiration());
        return LoginResponse.fromUser(user, accessTokenExpiry);
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshCookieValue = getCookieValue(request, REFRESH_TOKEN_COOKIE);
        if (refreshCookieValue != null && refreshCookieValue.contains(":")) {
            String tokenId = refreshCookieValue.split(":")[0];
            try {
                refreshTokenRepository.revokeFamily(
                        refreshTokenRepository.findById(Long.parseLong(tokenId))
                                .map(RefreshToken::getFamilyId).orElse(null));
            } catch (Exception e) {
                log.warn("Error during logout token revocation: {}", e.getMessage());
            }
        }
        revokeCookies(response);
    }

    @Override
    @Transactional
    public void logoutAllDevices(String userId, HttpServletResponse response) {
        refreshTokenRepository.revokeAllByUserId(userId);
        revokeCookies(response);
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Rate limit check by email
        PasswordResetRateLimiterService.RateLimitResult rateLimit = passwordResetRateLimiterService.tryConsume(email);
        if (!rateLimit.allowed()) {
            log.warn("Password reset rate limited for email: {}", email);
            // Always return generic message to not reveal if email exists
            return;
        }

        User user = userRepository.findUserByEmail(email);
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        // Generate OTP token
        String rawOtp = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        // Hash OTP before storing in DB (never store plain-text)
        user.setPasswordResetToken(passwordEncoder.encode(rawOtp));
        user.setPasswordResetTokenExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60L));
        user.setPasswordResetUsed(false);
        userRepository.save(user);

        // Send email with plain-text OTP (NOT logged anywhere)
        emailService.sendPasswordResetEmail(email, rawOtp);

        log.info("Password reset OTP sent to email: {}", email);
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        String rawOtp = request.getResetToken();
        String newPassword = request.getNewPassword();

        if (rawOtp == null || rawOtp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is required.");
        }

        validatePassword(newPassword);

        // Find all users with valid (non-expired, unused) reset tokens
        java.util.List<User> candidates = userRepository
                .findByPasswordResetTokenIsNotNullAndPasswordResetUsedFalseAndPasswordResetTokenExpiryAfter(
                        Instant.now());

        User matchedUser = null;
        for (User candidate : candidates) {
            if (passwordEncoder.matches(rawOtp, candidate.getPasswordResetToken())) {
                matchedUser = candidate;
                break;
            }
        }

        if (matchedUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token.");
        }

        // Mark token as used (single-use)
        matchedUser.setPasswordResetUsed(true);
        matchedUser.setPasswordResetToken(null);
        matchedUser.setPasswordResetTokenExpiry(null);

        // Update password
        matchedUser.setPassword(passwordEncoder.encode(newPassword));

        // Unlock account and revoke all sessions
        accountLockService.unlockAccount(matchedUser);
        refreshTokenRepository.revokeAllByUserId(matchedUser.getId());
        userRepository.save(matchedUser);

        log.info("Password reset successful for user {}", matchedUser.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }

        // Rate limit check
        PasswordChangeRateLimiterService.RateLimitResult rateLimit = passwordChangeRateLimiterService
                .tryConsume(userId);
        if (!rateLimit.allowed()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many password change attempts. Try again in " + rateLimit.retryAfterSeconds() + " seconds.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        validatePassword(request.getNewPassword());

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password and confirm password do not match.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must be different from current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password changed for user {}. All sessions revoked.", user.getEmail());
    }

    private String getClientIp(HttpServletRequest request) {
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

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void revokeCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie(ACCESS_TOKEN_COOKIE, "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setAttribute("SameSite", "Strict");

        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setAttribute("SameSite", "Strict");

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    private void revokeTokenFamilyByCookieValue(String cookieValue, HttpServletResponse response) {
        try {
            long tokenId = Long.parseLong(cookieValue.split(":")[0]);
            RefreshToken token = refreshTokenRepository.findById(tokenId).orElse(null);
            if (token != null) {
                refreshTokenRepository.revokeFamily(token.getFamilyId());
            }
        } catch (Exception ignored) {
        }
        revokeCookies(response);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters.");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least 1 uppercase letter and 1 number.");
        }
    }
}