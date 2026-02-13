package org.revature.revconnect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email Service - For development, just logs email content to console.
 * In production, this would integrate with an SMTP server or email provider.
 */
@Service
@Slf4j
public class EmailService {

    /**
     * Send a password reset email (mocked for development).
     * In production, this would send an actual email.
     *
     * @param toEmail    recipient email address
     * @param resetToken the password reset token
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        // In development, just log the token
        log.info("========================================");
        log.info("PASSWORD RESET EMAIL (MOCKED)");
        log.info("To: {}", toEmail);
        log.info("Reset Token: {}", resetToken);
        log.info("Reset Link: http://localhost:3000/reset-password?token={}", resetToken);
        log.info("========================================");
    }
}

