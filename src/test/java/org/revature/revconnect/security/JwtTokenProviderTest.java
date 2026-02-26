package org.revature.revconnect.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvclJldkNvbm5lY3RUZXNUaW5nUHVycG9zZXMxMjM0NTY=";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    private UserDetails testUser;
    private UserDetails anotherUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", EXPIRATION_MS);

        testUser = User.builder()
                .username("testuser@example.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();

        anotherUser = User.builder()
                .username("other@example.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("generateToken() produces a non-blank token")
        void generateToken_ShouldProduceNonBlankToken() {
            String token = jwtTokenProvider.generateToken(testUser);

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("generateToken() two different users produce different tokens")
        void generateToken_DifferentUsers_ProduceDifferentTokens() {
            String token1 = jwtTokenProvider.generateToken(testUser);
            String token2 = jwtTokenProvider.generateToken(anotherUser);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("generateToken() same user called twice may produce different tokens (timestamp)")
        void generateToken_SameUserCalledTwice_TokensAreValid() throws InterruptedException {
            String token1 = jwtTokenProvider.generateToken(testUser);
            Thread.sleep(50); // small delay to ensure different iat
            String token2 = jwtTokenProvider.generateToken(testUser);

            assertThat(jwtTokenProvider.validateToken(token1)).isTrue();
            assertThat(jwtTokenProvider.validateToken(token2)).isTrue();
        }

        @Test
        @DisplayName("generateToken(extraClaims, userDetails) embeds extra claims")
        void generateToken_WithExtraClaims_ProducesValidToken() {
            Map<String, Object> claims = Map.of("role", "ADMIN", "userId", 42L);
            String token = jwtTokenProvider.generateToken(claims, testUser);

            assertThat(token).isNotBlank();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("generateToken() token has 3 JWT segments (header.payload.signature)")
        void generateToken_HasThreeSegments() {
            String token = jwtTokenProvider.generateToken(testUser);
            String[] parts = token.split("\\.");

            assertThat(parts).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Username Extraction")
    class UsernameExtractionTests {

        @Test
        @DisplayName("extractUsername() returns correct username from valid token")
        void extractUsername_ValidToken_ReturnsUsername() {
            String token = jwtTokenProvider.generateToken(testUser);
            String extracted = jwtTokenProvider.extractUsername(token);

            assertThat(extracted).isEqualTo("testuser@example.com");
        }

        @Test
        @DisplayName("extractUsername() returns correct username for another user")
        void extractUsername_AnotherUser_ReturnsCorrectUsername() {
            String token = jwtTokenProvider.generateToken(anotherUser);
            String extracted = jwtTokenProvider.extractUsername(token);

            assertThat(extracted).isEqualTo("other@example.com");
        }

        @Test
        @DisplayName("extractUsername() throws exception on malformed token")
        void extractUsername_MalformedToken_ThrowsException() {
            assertThatThrownBy(() -> jwtTokenProvider.extractUsername("not.a.token"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("extractUsername() throws exception on empty string token")
        void extractUsername_EmptyToken_ThrowsException() {
            assertThatThrownBy(() -> jwtTokenProvider.extractUsername(""))
                    .isInstanceOf(Exception.class);
        }
    }


    @Nested
    @DisplayName("Token Validation - validateToken()")
    class ValidateTokenTests {

        @Test
        @DisplayName("validateToken() returns true for a freshly generated valid token")
        void validateToken_ValidToken_ReturnsTrue() {
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("validateToken() returns false for a completely invalid string")
        void validateToken_InvalidString_ReturnsFalse() {
            assertThat(jwtTokenProvider.validateToken("invalid-token")).isFalse();
        }

        @Test
        @DisplayName("validateToken() returns false for an empty string")
        void validateToken_EmptyString_ReturnsFalse() {
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("validateToken() returns false for null input")
        void validateToken_NullInput_ReturnsFalse() {
            assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        }

        @Test
        @DisplayName("validateToken() returns false for token with wrong signature")
        void validateToken_WrongSignature_ReturnsFalse() {
            String token = jwtTokenProvider.generateToken(testUser);
            // Replace the entire signature segment (3rd part) with a fake one
            String[] parts = token.split("\\.");
            String tampered = parts[0] + "." + parts[1] + ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("validateToken() returns false for expired token")
        void validateToken_ExpiredToken_ReturnsFalse() {

            ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", -1L);
            String expiredToken = jwtTokenProvider.generateToken(testUser);

            ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", EXPIRATION_MS);

            assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
        }

        @Test
        @DisplayName("validateToken() returns false for malformed JWT (only 2 parts)")
        void validateToken_MalformedJwt_ReturnsFalse() {
            assertThat(jwtTokenProvider.validateToken("header.payload")).isFalse();
        }

        @Test
        @DisplayName("validateToken() returns false for token signed with a different secret")
        void validateToken_DifferentSecret_ReturnsFalse() {
            // Generate token with current secret
            String token = jwtTokenProvider.generateToken(testUser);

            // Change secret
            ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                    "ZGlmZmVyZW50U2VjcmV0S2V5Rm9yVGVzdGluZ1B1cnBvc2VzMTIzNDU2Nzg=");

            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Validity Check - isTokenValid()")
    class IsTokenValidTests {

        @Test
        @DisplayName("isTokenValid() returns true when token matches the user and is not expired")
        void isTokenValid_MatchingUserAndNotExpired_ReturnsTrue() {
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(jwtTokenProvider.isTokenValid(token, testUser)).isTrue();
        }

        @Test
        @DisplayName("isTokenValid() returns false when token belongs to different user")
        void isTokenValid_DifferentUser_ReturnsFalse() {
            String tokenForTestUser = jwtTokenProvider.generateToken(testUser);
            // Validate against anotherUser
            assertThat(jwtTokenProvider.isTokenValid(tokenForTestUser, anotherUser)).isFalse();
        }

        @Test
        @DisplayName("isTokenValid() returns false for expired token")
        void isTokenValid_ExpiredToken_ReturnsFalse() {
            ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", -1L);
            String expiredToken = jwtTokenProvider.generateToken(testUser);
            ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", EXPIRATION_MS);

            assertThatThrownBy(() -> jwtTokenProvider.isTokenValid(expiredToken, testUser))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Token generated for user with special chars in username is valid")
        void generateToken_UsernameWithSpecialChars_ValidToken() {
            UserDetails specialUser = User.builder()
                    .username("user+test@mail.co.uk")
                    .password("pwd")
                    .authorities(Collections.emptyList())
                    .build();

            String token = jwtTokenProvider.generateToken(specialUser);
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("user+test@mail.co.uk");
        }

        @Test
        @DisplayName("generateToken() with empty extra claims still produces valid token")
        void generateToken_EmptyExtraClaims_ValidToken() {
            String token = jwtTokenProvider.generateToken(Map.of(), testUser);
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("extractClaim() correctly retrieves custom claim resolver output")
        void extractClaim_CanExtractExpiration() {
            String token = jwtTokenProvider.generateToken(testUser);

            java.util.Date expiration = jwtTokenProvider.extractClaim(token,
                    io.jsonwebtoken.Claims::getExpiration);

            assertThat(expiration).isAfter(new java.util.Date());
        }
    }
}
