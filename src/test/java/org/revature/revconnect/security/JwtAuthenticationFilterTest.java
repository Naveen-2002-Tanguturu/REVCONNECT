package org.revature.revconnect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUserDetails = User.builder()
                .username("testuser@example.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();

        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("No Token in Request")
    class NoTokenTests {

        @Test
        @DisplayName("doFilter: no Authorization header → filter chain is called, no authentication set")
        void doFilter_NoAuthorizationHeader_ShouldPassThroughWithoutAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider);
            verifyNoInteractions(userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("doFilter: empty Authorization header → filter chain is called")
        void doFilter_EmptyAuthorizationHeader_ShouldPassThrough() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("");

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider);
        }

        @Test
        @DisplayName("doFilter: Authorization header without 'Bearer ' prefix → no auth set")
        void doFilter_AuthHeaderWithoutBearerPrefix_ShouldPassThrough() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("doFilter: Authorization header with 'Bearer' but no space → no auth set")
        void doFilter_BearerWithoutSpace_ShouldPassThrough() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearertoken123");

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider);
        }
    }

    @Nested
    @DisplayName("Invalid Token")
    class InvalidTokenTests {

        @Test
        @DisplayName("doFilter: invalid JWT → filter chain called, no authentication set")
        void doFilter_InvalidJwt_ShouldPassThroughWithoutAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid.jwt.token");
            when(jwtTokenProvider.validateToken("invalid.jwt.token")).thenReturn(false);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenProvider).validateToken("invalid.jwt.token");
            verifyNoInteractions(userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("doFilter: expired JWT → validateToken returns false → no auth set")
        void doFilter_ExpiredJwt_ShouldPassThroughWithoutAuth() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer expired.jwt.token");
            when(jwtTokenProvider.validateToken("expired.jwt.token")).thenReturn(false);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("doFilter: token passes validateToken but isTokenValid fails → no auth set")
        void doFilter_TokenPassesValidateButIsTokenValidFails_ShouldNotSetAuth() throws Exception {
            String jwt = "valid.structure.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
            when(jwtTokenProvider.extractUsername(jwt)).thenReturn("testuser@example.com");
            when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(testUserDetails);
            when(jwtTokenProvider.isTokenValid(jwt, testUserDetails)).thenReturn(false);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("doFilter: exception thrown during token validation → filter chain still called")
        void doFilter_ExceptionDuringValidation_ShouldStillCallFilterChain() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer bad.token.here");
            when(jwtTokenProvider.validateToken("bad.token.here"))
                    .thenThrow(new RuntimeException("Unexpected error"));

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Filter chain must still be called even on exception
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Valid Token")
    class ValidTokenTests {

        @Test
        @DisplayName("doFilter: valid JWT → SecurityContext is populated with authentication")
        void doFilter_ValidJwt_ShouldSetAuthenticationInContext() throws Exception {
            String jwt = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
            when(jwtTokenProvider.extractUsername(jwt)).thenReturn("testuser@example.com");
            when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(testUserDetails);
            when(jwtTokenProvider.isTokenValid(jwt, testUserDetails)).thenReturn(true);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                    .isEqualTo("testuser@example.com");
        }

        @Test
        @DisplayName("doFilter: valid JWT → UserDetailsService is called once")
        void doFilter_ValidJwt_ShouldCallUserDetailsServiceOnce() throws Exception {
            String jwt = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
            when(jwtTokenProvider.extractUsername(jwt)).thenReturn("testuser@example.com");
            when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(testUserDetails);
            when(jwtTokenProvider.isTokenValid(jwt, testUserDetails)).thenReturn(true);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(userDetailsService, times(1)).loadUserByUsername("testuser@example.com");
        }

        @Test
        @DisplayName("doFilter: valid JWT → filter chain is always called after auth setup")
        void doFilter_ValidJwt_ShouldAlwaysCallFilterChain() throws Exception {
            String jwt = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
            when(jwtTokenProvider.extractUsername(jwt)).thenReturn("testuser@example.com");
            when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(testUserDetails);
            when(jwtTokenProvider.isTokenValid(jwt, testUserDetails)).thenReturn(true);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("doFilter: valid JWT → Authentication principal is the UserDetails object")
        void doFilter_ValidJwt_PrincipalIsUserDetails() throws Exception {
            String jwt = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
            when(jwtTokenProvider.extractUsername(jwt)).thenReturn("testuser@example.com");
            when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(testUserDetails);
            when(jwtTokenProvider.isTokenValid(jwt, testUserDetails)).thenReturn(true);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal).isInstanceOf(UserDetails.class);
            assertThat(((UserDetails) principal).getUsername()).isEqualTo("testuser@example.com");
        }

        @Test
        @DisplayName("doFilter: 'Bearer ' prefix is correctly stripped from token")
        void doFilter_BearerTokenPrefixIsStripped() throws Exception {
            String jwt = "actual.jwt.value";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
            when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
            when(jwtTokenProvider.extractUsername(jwt)).thenReturn("testuser@example.com");
            when(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(testUserDetails);
            when(jwtTokenProvider.isTokenValid(jwt, testUserDetails)).thenReturn(true);

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Verify that validateToken was called with the raw token, not "Bearer token"
            verify(jwtTokenProvider).validateToken(jwt);
            verify(jwtTokenProvider, never()).validateToken("Bearer " + jwt);
        }
    }
}
