package org.revature.revconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send a password reset email using Gmail SMTP.
     *
     * @param toEmail recipient email address
     * @param otp     the 6-digit OTP
     */
    public void sendPasswordResetEmail(String toEmail, String otp) {
        log.info("Attempting to send real OTP email to: {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("ganeshchinamana39@gmail.com");
            message.setTo(toEmail);
            message.setSubject("RevConnect - Password Reset OTP");
            message.setText("Hello,\n\n" +
                    "You have requested to reset your password. Use the following 6-digit OTP to proceed:\n\n" +
                    "OTP: " + otp + "\n\n" +
                    "This OTP is valid for 24 hours.\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "RevConnect Team");

            mailSender.send(message);
            log.info("OTP email successfully sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to send real email to {}: {}", toEmail, e.getMessage());
            log.info("========================================");
            log.info("FALLBACK OTP (Console): {}", otp);
            log.info("========================================");
        }
    }

    /**
     * Send an account verification email using Gmail SMTP.
     *
     * @param toEmail recipient email address
     * @param otp     the 6-digit OTP
     */
    public void sendVerificationEmail(String toEmail, String otp) {
        log.info("Attempting to send Verification OTP email to: {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("ganeshchinamana39@gmail.com");
            message.setTo(toEmail);
            message.setSubject("RevConnect - Verify Your Account");
            message.setText("Welcome to RevConnect!\n\n" +
                    "To complete your registration and verify your email address, please use the following 6-digit OTP:\n\n"
                    +
                    "OTP: " + otp + "\n\n" +
                    "This OTP is valid for 24 hours.\n\n" +
                    "Best regards,\n" +
                    "RevConnect Team");

            mailSender.send(message);
            log.info("Verification OTP email successfully sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to send verify email to {}: {}", toEmail, e.getMessage());
            log.info("========================================");
            log.info("FALLBACK VERIFY OTP (Console): {}", otp);
            log.info("========================================");
        }
    }
}
