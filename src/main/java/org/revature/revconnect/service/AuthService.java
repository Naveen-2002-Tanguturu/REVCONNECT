package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.ForgotPasswordRequest;
import org.revature.revconnect.dto.request.LoginRequest;
import org.revature.revconnect.dto.request.RegisterRequest;
import org.revature.revconnect.dto.request.ResetPasswordRequest;
import org.revature.revconnect.dto.response.AuthResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.DuplicateResourceException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.PasswordResetToken;
import org.revature.revconnect.model.User;
import org.revature.revconnect.model.UserSettings;
import org.revature.revconnect.repository.PasswordResetTokenRepository;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.repository.UserSettingsRepository;
import org.revature.revconnect.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    private static final int TOKEN_EXPIRY_HOURS = 24;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .userType(request.getUserType())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Create default user settings
        UserSettings settings = UserSettings.builder()
                .user(savedUser)
                .build();
        userSettingsRepository.save(settings);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(savedUser);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .userType(savedUser.getUserType())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsernameOrEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(user);

        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .build();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("No authenticated user found");
        }
        return (User) authentication.getPrincipal();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Delete any existing token for this user
        passwordResetTokenRepository.findByUser(user)
                .ifPresent(token -> passwordResetTokenRepository.delete(token));

        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset token created for user: {}", user.getUsername());

        // Send email (mocked in dev)
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset for token");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete the used token
        passwordResetTokenRepository.delete(resetToken);

        log.info("Password reset successful for user: {}", user.getUsername());
    }
}
