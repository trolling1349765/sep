package fpt.capstone.service;

import fpt.capstone.dto.request.ChangePasswordRequest;
import fpt.capstone.dto.request.LoginRequest;
import fpt.capstone.dto.request.PasswordResetConfirmRequest;
import fpt.capstone.dto.request.PasswordResetRequest;
import fpt.capstone.dto.request.RegisterRequest;
import fpt.capstone.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthService {

    LoginResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    LoginResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    void requestPasswordReset(PasswordResetRequest request);

    void confirmPasswordReset(PasswordResetConfirmRequest request);

    void changePassword(String userId, ChangePasswordRequest request);

    void logoutAllDevices(String userId, HttpServletResponse response);

    void generateCaptcha(HttpServletResponse response, Map<String, String> captchaStore);
}
