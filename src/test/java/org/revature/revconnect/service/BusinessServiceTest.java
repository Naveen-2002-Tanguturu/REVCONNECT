package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.BusinessProfileRequest;
import org.revature.revconnect.dto.response.AnalyticsResponse;
import org.revature.revconnect.dto.response.BusinessProfileResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.enums.BusinessCategory;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.BusinessProfile;
import org.revature.revconnect.mapper.BusinessProfileMapper;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.BusinessProfileRepository;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostAnalyticsRepository;
import org.revature.revconnect.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    @Mock
    private BusinessProfileRepository businessProfileRepository;
    @Mock
    private PostAnalyticsRepository postAnalyticsRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private AuthService authService;

    @Mock
    private BusinessProfileMapper businessProfileMapper;

    @InjectMocks
    private BusinessService businessService;

    private User testUser;
    private BusinessProfile testProfile;
    private BusinessProfileRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("businessuser")
                .email("business@example.com")
                .name("Business User")
                .build();

        testProfile = BusinessProfile.builder()
                .id(1L)
                .user(testUser)
                .businessName("Test Business")
                .category(BusinessCategory.TECHNOLOGY)
                .description("A test business")
                .websiteUrl("https://example.com")
                .contactEmail("contact@example.com")
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        testRequest = BusinessProfileRequest.builder()
                .businessName("Test Business")
                .category(BusinessCategory.TECHNOLOGY)
                .description("A test business")
                .websiteUrl("https://example.com")
                .contactEmail("contact@example.com")
                .build();

        // Common mapper stubbing
        lenient().when(businessProfileMapper.toResponse(any(BusinessProfile.class))).thenAnswer(invocation -> {
            BusinessProfile p = invocation.getArgument(0);
            return BusinessProfileResponse.builder().id(p.getId()).businessName(p.getBusinessName()).build();
        });
    }

    @Test
    void createBusinessProfile_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(businessProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(testProfile);

        BusinessProfileResponse response = businessService.createBusinessProfile(testRequest);

        assertNotNull(response);
        assertEquals("Test Business", response.getBusinessName());
        verify(businessProfileRepository).save(any(BusinessProfile.class));
    }

    @Test
    void createBusinessProfile_AlreadyExists() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(businessProfileRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> businessService.createBusinessProfile(testRequest));
    }

    @Test
    void getBusinessProfile_Success() {
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        BusinessProfileResponse response = businessService.getBusinessProfile(1L);

        assertNotNull(response);
        assertEquals("Test Business", response.getBusinessName());
    }

    @Test
    void getBusinessProfile_NotFound() {
        when(businessProfileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> businessService.getBusinessProfile(999L));
    }

    @Test
    void updateBusinessProfile_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(testProfile);

        BusinessProfileResponse response = businessService.updateBusinessProfile(testRequest);

        assertNotNull(response);
        verify(businessProfileRepository).save(any(BusinessProfile.class));
    }

    @Test
    void deleteBusinessProfile_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        assertDoesNotThrow(() -> businessService.deleteBusinessProfile());
        verify(businessProfileRepository).delete(testProfile);
    }

    @Test
    void getBusinessesByCategory_Success() {
        Page<BusinessProfile> page = new PageImpl<>(List.of(testProfile));
        when(businessProfileRepository.findByCategory(eq(BusinessCategory.TECHNOLOGY), any(PageRequest.class)))
                .thenReturn(page);

        PagedResponse<BusinessProfileResponse> response = businessService.getBusinessesByCategory(
                BusinessCategory.TECHNOLOGY, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void searchBusinesses_Success() {
        Page<BusinessProfile> page = new PageImpl<>(List.of(testProfile));
        when(businessProfileRepository.findByBusinessNameContainingIgnoreCase(eq("Test"), any(PageRequest.class)))
                .thenReturn(page);

        PagedResponse<BusinessProfileResponse> response = businessService.searchBusinesses("Test", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getAnalytics_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postAnalyticsRepository.findByUserIdAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(postAnalyticsRepository.getTotalViewsByUser(1L)).thenReturn(100L);
        when(postAnalyticsRepository.getTotalImpressionsByUser(1L)).thenReturn(500L);
        when(connectionRepository.countByFollowingIdAndStatus(1L, ConnectionStatus.ACCEPTED)).thenReturn(50L);
        when(postRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AnalyticsResponse response = businessService.getAnalytics(30);

        assertNotNull(response);
        assertEquals(100L, response.getTotalViews());
        assertEquals(50L, response.getTotalFollowers());
    }

    // ======================== EDGE CASES ========================

    @Test
    void updateBusinessProfile_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> businessService.updateBusinessProfile(testRequest));
        verify(businessProfileRepository, never()).save(any());
    }

    @Test
    void deleteBusinessProfile_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> businessService.deleteBusinessProfile());
        verify(businessProfileRepository, never()).delete(any());
    }

    @Test
    void getMyBusinessProfile_DelegatesToGetBusinessProfile() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(testProfile));

        BusinessProfileResponse response = businessService.getMyBusinessProfile();

        assertNotNull(response);
        assertEquals("Test Business", response.getBusinessName());
        verify(businessProfileRepository).findByUserId(1L);
    }

    @Test
    void getBusinessesByCategory_ReturnsEmpty() {
        Page<BusinessProfile> emptyPage = new PageImpl<>(List.of());
        when(businessProfileRepository.findByCategory(eq(BusinessCategory.TECHNOLOGY), any(PageRequest.class)))
                .thenReturn(emptyPage);

        PagedResponse<BusinessProfileResponse> response = businessService.getBusinessesByCategory(
                BusinessCategory.TECHNOLOGY, 0, 10);

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getContent().isEmpty());
    }

    @Test
    void searchBusinesses_ReturnsEmpty() {
        Page<BusinessProfile> emptyPage = new PageImpl<>(List.of());
        when(businessProfileRepository.findByBusinessNameContainingIgnoreCase(eq("none"), any(PageRequest.class)))
                .thenReturn(emptyPage);

        PagedResponse<BusinessProfileResponse> response = businessService.searchBusinesses("none", 0, 10);

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
    }
}
