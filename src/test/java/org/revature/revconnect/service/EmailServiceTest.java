package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendPasswordResetEmail_Success() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail("test@example.com", "reset-token-123"));
    }

    @Test
    void sendPasswordResetEmail_WithNullEmail() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(null, "reset-token-123"));
    }

    @Test
    void sendPasswordResetEmail_WithNullToken() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail("test@example.com", null));
    }
}
