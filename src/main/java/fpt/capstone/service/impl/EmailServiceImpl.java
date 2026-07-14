package fpt.capstone.service.impl;

import fpt.capstone.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String otpToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - Social Support Portal");

            String emailContent = buildEmailContent(otpToken);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email. Please try again later.", e);
        }
    }

    private String buildEmailContent(String otpToken) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <h2 style="color: #333333; text-align: center;">Password Reset Request</h2>
                        <p style="color: #555555; font-size: 14px; line-height: 1.6;">
                            You have requested to reset your password. Please use the OTP code below to complete the password reset process.
                        </p>
                        <div style="text-align: center; margin: 30px 0;">
                            <div style="display: inline-block; background-color: #f8f9fa; border: 2px dashed #1976d2; border-radius: 8px; padding: 15px 30px; font-size: 24px; font-weight: bold; letter-spacing: 4px; color: #1976d2; font-family: 'Courier New', monospace;">
                                %s
                            </div>
                        </div>
                        <p style="color: #555555; font-size: 14px; line-height: 1.6;">
                            This OTP code will expire in <strong>8 minutes</strong>. If you did not request a password reset, please ignore this email.
                        </p>
                        <p style="color: #555555; font-size: 14px; line-height: 1.6;">
                            For security reasons, do not share this OTP code with anyone.
                        </p>
                        <hr style="border: none; border-top: 1px solid #eeeeee; margin: 20px 0;">
                        <p style="color: #999999; font-size: 12px; text-align: center;">
                            This is an automated message from the Social Support Portal. Please do not reply to this email.
                        </p>
                    </div>
                </body>
                </html>
                """
                .formatted(otpToken);
    }
}