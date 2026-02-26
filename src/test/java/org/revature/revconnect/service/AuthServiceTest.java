package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.ForgotPasswordRequest;
import org.revature.revconnect.dto.request.LoginRequest;
import org.revature.revconnect.dto.request.RegisterRequest;
import org.revature.revconnect.dto.request.ResetPasswordRequest;
import org.revature.revconnect.dto.response.AuthResponse;
import org.revature.revconnect.enums.UserType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .userType(UserType.PERSONAL)
                .build();

        loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .name("Test User")
                .userType(UserType.PERSONAL)
                .build();
    }

    // ======================== REGISTER ========================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void register_Success() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(new UserSettings());
            when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getUserType()).isEqualTo(UserType.PERSONAL);
            verify(userRepository).save(any(User.class));
            verify(userSettingsRepository).save(any(UserSettings.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when username exists")
        void register_DuplicateUsername() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when email exists")
        void register_DuplicateEmail() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ======================== LOGIN ========================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void login_Success() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt-token");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }
    }

    // ======================== GET CURRENT USER ========================

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should throw BadRequestException when authentication is null")
        void getCurrentUser_NullAuthentication_ThrowsBadRequest() {
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(ctx);

            assertThatThrownBy(() -> authService.getCurrentUser())
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No authenticated user found");

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should throw BadRequestException when authentication is not authenticated")
        void getCurrentUser_NotAuthenticated_ThrowsBadRequest() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);

            assertThatThrownBy(() -> authService.getCurrentUser())
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No authenticated user found");

            SecurityContextHolder.clearContext();
        }
    }

    // ======================== FORGOT PASSWORD ========================

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when email not found")
        void forgotPassword_EmailNotFound_ThrowsException() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("unknown@example.com")
                    .build();
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("Should delete existing reset token before creating new one")
        void forgotPassword_DeletesExistingToken() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("test@example.com")
                    .build();
            PasswordResetToken existingToken = PasswordResetToken.builder()
                    .token("old-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordResetTokenRepository.findByUser(testUser)).thenReturn(Optional.of(existingToken));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(existingToken);

            authService.forgotPassword(request);

            verify(passwordResetTokenRepository).delete(existingToken);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("Should create new token when no existing token")
        void forgotPassword_NoExistingToken_CreatesNew() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("test@example.com")
                    .build();
            PasswordResetToken savedToken = PasswordResetToken.builder()
                    .token("new-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().plusHours(24))
                    .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordResetTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(savedToken);

            authService.forgotPassword(request);

            verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
        }
    }

    // ======================== RESET PASSWORD ========================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should throw BadRequestException when token not found")
        void resetPassword_TokenNotFound_ThrowsException() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("invalid-token")
                    .newPassword("NewPass123!")
                    .build();
            when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid or expired reset token");
        }

        @Test
        @DisplayName("Should throw BadRequestException and delete token when expired")
        void resetPassword_ExpiredToken_ThrowsExceptionAndDeletesToken() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("expired-token")
                    .newPassword("NewPass123!")
                    .build();
            PasswordResetToken expiredToken = PasswordResetToken.builder()
                    .token("expired-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().minusHours(1)) // already expired
                    .build();

            when(passwordResetTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");

            verify(passwordResetTokenRepository).delete(expiredToken);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should reset password successfully with valid non-expired token")
        void resetPassword_ValidToken_Success() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("NewPass123!")
                    .build();
            PasswordResetToken validToken = PasswordResetToken.builder()
                    .token("valid-token")
                    .user(testUser)
                    .expiryDate(LocalDateTime.now().plusHours(23)) // not expired
                    .build();

            when(passwordResetTokenRepository.findByToken("valid-token"))
                    .thenReturn(Optional.of(validToken));
            when(passwordEncoder.encode("NewPass123!")).thenReturn("encodedNewPass");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.resetPassword(request);

            verify(userRepository).save(testUser);
            verify(passwordResetTokenRepository).delete(validToken);
        }
    }
}
