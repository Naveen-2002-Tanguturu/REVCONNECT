package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EmailServiceTest {

    private final EmailService emailService = new EmailService();

    @Test
    void sendPasswordResetEmail_withValidInputs_doesNotThrow() {
        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("user@test.com", "token-123"));
    }

    @Test
    void sendPasswordResetEmail_withNullInputs_doesNotThrow() {
        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail(null, null));
    }
}
