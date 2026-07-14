package fpt.capstone.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String otpToken);
}