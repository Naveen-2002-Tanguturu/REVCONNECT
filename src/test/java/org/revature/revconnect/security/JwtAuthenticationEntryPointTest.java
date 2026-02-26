package org.revature.revconnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint Tests")
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint();
    }

    @Test
    @DisplayName("commence() sends 401 Unauthorized error for BadCredentialsException")
    void commence_BadCredentials_Sends401() throws IOException {
        AuthenticationException ex = new BadCredentialsException("Bad credentials");

        entryPoint.commence(request, response, ex);

        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Unauthorized: Bad credentials"));
    }

    @Test
    @DisplayName("commence() sends 401 Unauthorized for InsufficientAuthenticationException")
    void commence_InsufficientAuthentication_Sends401() throws IOException {
        AuthenticationException ex = new InsufficientAuthenticationException("Full authentication required");

        entryPoint.commence(request, response, ex);

        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Unauthorized: Full authentication required"));
    }

    @Test
    @DisplayName("commence() sends 401 with null message when exception has null message")
    void commence_NullExceptionMessage_Sends401WithNullMessage() throws IOException {
        AuthenticationException ex = new BadCredentialsException(null);

        entryPoint.commence(request, response, ex);

        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Unauthorized: null"));
    }

    @Test
    @DisplayName("commence() HTTP status code is exactly 401 (SC_UNAUTHORIZED = 401)")
    void commence_StatusCodeIs401() throws IOException {
        AuthenticationException ex = new BadCredentialsException("Unauthorized");

        entryPoint.commence(request, response, ex);

        verify(response).sendError(eq(401), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("commence() works for any AuthenticationException subtype")
    void commence_GenericAuthenticationException_Sends401() throws IOException {
        AuthenticationException ex = new InsufficientAuthenticationException("Token missing");

        entryPoint.commence(request, response, ex);

        verify(response).sendError(
                eq(HttpServletResponse.SC_UNAUTHORIZED),
                eq("Unauthorized: Token missing"));
    }
}
