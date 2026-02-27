package org.revature.revconnect.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.controller.*;
import org.revature.revconnect.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.revature.revconnect.config.SecurityConfig;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.mapper.PostMapper;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        PostController.class,
        UserController.class,
        HashtagController.class,
        AdminController.class,
        AnalyticsController.class,
        SettingsController.class,
        SearchController.class
})
@Import(SecurityConfig.class)
@DisplayName("Security Configuration Integration Tests")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private PostService postService;
    @MockBean
    private UserService userService;
    @MockBean
    private HashtagService hashtagService;
    @MockBean
    private BusinessService businessService;
    @MockBean
    private NotificationService notificationService;
    @MockBean
    private BookmarkService bookmarkService;
    @MockBean
    private ConnectionService connectionService;
    @MockBean
    private MessageService messageService;
    @MockBean
    private StoryService storyService;
    @MockBean
    private InteractionService interactionService;

    @MockBean
    private UserMapper userMapper;
    @MockBean
    private PostMapper postMapper;

    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() throws Exception {
        testUserDetails = User.builder()
                .username("testuser@example.com")
                .password("encoded-pass")
                .authorities(Collections.emptyList())
                .build();

        doAnswer(invocation -> {
            HttpServletResponse resp = invocation.getArgument(1);
            AuthenticationException ex = invocation.getArgument(2);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: " + (ex != null ? ex.getMessage() : ""));
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.extractUsername(anyString())).thenReturn("testuser@example.com");
        when(jwtTokenProvider.isTokenValid(anyString(), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("testuser@example.com"))
                .thenReturn(testUserDetails);
    }

    @Nested
    @DisplayName("Public Endpoints — No Token Required")
    class PublicEndpointTests {

        @Test
        @DisplayName("POST /api/auth/login → not 401 (public)")
        void authLogin_IsPublic() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"usernameOrEmail\":\"u\",\"password\":\"p\"}"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/auth/login"));
        }

        @Test
        @DisplayName("POST /api/auth/register → not 401 (public)")
        void authRegister_IsPublic() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType("application/json")
                            .content(
                                    "{\"username\":\"u\",\"email\":\"e@e.com\",\"password\":\"P@ss1\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/auth/register"));
        }

        @Test
        @DisplayName("POST /api/auth/forgot-password → not 401 (public)")
        void authForgotPassword_IsPublic() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType("application/json")
                            .content("{\"email\":\"test@example.com\"}"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(),
                            "/api/auth/forgot-password"));
        }

        @Test
        @DisplayName("POST /api/auth/logout → not 401 (public via /api/auth/**)")
        void authLogout_IsPublic() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/auth/logout"));
        }

        @Test
        @DisplayName("GET /api/auth/me → not 401 (public via /api/auth/**)")
        void authMe_IsPublic() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/auth/me"));
        }
    }

    @Nested
    @DisplayName("Protected Endpoints — Return 401 Without Token")
    class ProtectedWithoutTokenTests {

        @BeforeEach
        void disableTokenValidation() {
            // Override: all tokens are invalid → simulates no valid auth
            when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);
        }

        @Test
        @DisplayName("GET /api/users/me → 401 without token")
        void usersMe_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/admin/users → 401 without token")
        void adminUsers_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/analytics/overview → 401 without token")
        void analyticsOverview_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/api/analytics/overview"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/settings → 401 without token")
        void settings_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/api/settings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/posts → 401 without token")
        void createPost_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(post("/api/posts")
                            .contentType("application/json")
                            .content("{\"content\":\"hello\",\"type\":\"TEXT\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/search/all → 401 without token")
        void searchAll_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/api/search/all").param("query", "test"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/hashtags/test/posts → 401 without token")
        void hashtagPosts_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/api/hashtags/test/posts"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Protected Endpoints — Accessible With Valid JWT")
    class ProtectedWithValidTokenTests {

        @Test
        @DisplayName("GET /api/users/me → not 401 with valid Bearer token")
        void usersMe_WithValidToken_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer any-valid-token"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/users/me"));
        }

        @Test
        @DisplayName("GET /api/admin/users → not 401 with valid Bearer token")
        void adminUsers_WithValidToken_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer any-valid-token"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/admin/users"));
        }

        @Test
        @DisplayName("GET /api/analytics/summary → not 401 with valid Bearer token")
        void analytics_WithValidToken_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/analytics/summary")
                            .header("Authorization", "Bearer any-valid-token"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(),
                            "/api/analytics/summary"));
        }
    }
    @Nested
    @DisplayName("Tampered or Invalid Token")
    class InvalidTokenTests {

        @Test
        @DisplayName("Tampered token on protected endpoint → 401")
        void tamperedToken_ProtectedEndpoint_Returns401() throws Exception {
            when(jwtTokenProvider.validateToken("tampered-token")).thenReturn(false);

            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer tampered-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token with wrong signature → 401")
        void wrongSignatureToken_Returns401() throws Exception {
            when(jwtTokenProvider.validateToken("wrong.sig.token")).thenReturn(false);

            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer wrong.sig.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("No Authorization header → 401 on protected endpoint")
        void noAuthHeader_ProtectedEndpoint_Returns401() throws Exception {
            when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

            mockMvc.perform(get("/api/settings"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Authenticated User — @WithMockUser")
    class WithMockUserTests {

        @Test
        @WithMockUser(username = "user@test.com", roles = "USER")
        @DisplayName("GET /api/users/me → not 401 when @WithMockUser present")
        void usersMe_WithMockUser_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/users/me"));
        }

        @Test
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        @DisplayName("GET /api/admin/users → not 401 with mock ADMIN user")
        void adminUsers_WithMockAdminUser_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/admin/users"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/analytics/overview → not 401 with any authenticated user")
        void analyticsOverview_WithMockUser_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/analytics/overview"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(),
                            "/api/analytics/overview"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/settings/preferences → not 401 with authenticated user")
        void settingsPreferences_WithMockUser_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/settings/preferences"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(),
                            "/api/settings/preferences"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/search/all → not 401 with authenticated user")
        void searchAll_WithMockUser_NotUnauthorized() throws Exception {
            mockMvc.perform(get("/api/search/all").param("query", "java"))
                    .andExpect(result -> assertNotUnauthorized(result.getResponse().getStatus(), "/api/search/all"));
        }
    }


    @Nested
    @DisplayName("CSRF Protection — Disabled")
    class CsrfTests {

        @Test
        @DisplayName("POST /api/auth/login without CSRF token → not 403 (CSRF is disabled)")
        void postWithoutCsrf_ShouldNotReturn403() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"usernameOrEmail\":\"u\",\"password\":\"p\"}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status != 403 : "CSRF is disabled — POST should not return 403, got: " + status;
                    });
        }

        @Test
        @WithMockUser
        @DisplayName("PUT without CSRF token → not 403 (CSRF is disabled)")
        void putWithoutCsrf_ShouldNotReturn403() throws Exception {
            mockMvc.perform(put("/api/settings")
                            .contentType("application/json")
                            .content("{\"language\":\"en\"}"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status != 403 : "CSRF is disabled — PUT should not return 403, got: " + status;
                    });
        }

        @Test
        @WithMockUser
        @DisplayName("DELETE without CSRF token → not 403 (CSRF is disabled)")
        void deleteWithoutCsrf_ShouldNotReturn403() throws Exception {
            mockMvc.perform(delete("/api/search/recent"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status != 403 : "CSRF is disabled — DELETE should not return 403, got: " + status;
                    });
        }
    }

    @Nested
    @DisplayName("Stateless Session — No JSESSIONID")
    class StatelessSessionTests {

        @Test
        @DisplayName("Login response should not set JSESSIONID cookie")
        void loginResponse_ShouldNotHaveJsessionidCookie() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"usernameOrEmail\":\"u\",\"password\":\"p\"}"))
                    .andExpect(result -> {
                        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
                        if (setCookieHeader != null) {
                            assert !setCookieHeader.contains("JSESSIONID")
                                    : "Stateless session: JSESSIONID should NOT be set in response cookie";
                        }
                    });
        }

        @Test
        @WithMockUser
        @DisplayName("Authenticated request response should not set JSESSIONID cookie")
        void authenticatedResponse_ShouldNotHaveJsessionidCookie() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(result -> {
                        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
                        if (setCookieHeader != null) {
                            assert !setCookieHeader.contains("JSESSIONID")
                                    : "Stateless session: JSESSIONID should NOT be set in response cookie";
                        }
                    });
        }
    }

    private void assertNotUnauthorized(int status, String endpoint) {
        assert status != 401
                : "Expected endpoint [" + endpoint + "] to NOT return 401, but got: " + status;
    }
}
