package org.revature.revconnect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.revature.revconnect.security.JwtTokenProvider;
import org.revature.revconnect.security.JwtAuthenticationEntryPoint;
import org.revature.revconnect.security.JwtAuthenticationFilter;
import org.revature.revconnect.service.AnalyticsService;
import org.springframework.security.core.userdetails.UserDetailsService;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @WithMockUser
    public void testGetOverview() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetProfileViews() throws Exception {
        mockMvc.perform(get("/api/analytics/profile-views"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetPostPerformance() throws Exception {
        mockMvc.perform(get("/api/analytics/post-performance"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetFollowerGrowth() throws Exception {
        mockMvc.perform(get("/api/analytics/followers/growth"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetTopPosts() throws Exception {
        mockMvc.perform(get("/api/analytics/top-posts"))
                .andExpect(status().isOk());
    }
}
