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
import org.revature.revconnect.model.PostAnalytics;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.BusinessProfileRepository;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostAnalyticsRepository;
import org.revature.revconnect.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessProfileRepository businessProfileRepository;
    private final PostAnalyticsRepository postAnalyticsRepository;
    private final PostRepository postRepository;
    private final ConnectionRepository connectionRepository;
    private final AuthService authService;

    @Transactional
    public BusinessProfileResponse createBusinessProfile(BusinessProfileRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("Creating business profile for user: {}", currentUser.getUsername());

        if (businessProfileRepository.existsByUserId(currentUser.getId())) {
            log.warn("User {} already has a business profile", currentUser.getUsername());
            throw new BadRequestException("You already have a business profile");
        }

        BusinessProfile profile = BusinessProfile.builder()
                .user(currentUser)
                .businessName(request.getBusinessName())
                .category(request.getCategory())
                .description(request.getDescription())
                .websiteUrl(request.getWebsiteUrl())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .logoUrl(request.getLogoUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .build();

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Business profile created with ID: {}", saved.getId());
        return BusinessProfileResponse.fromEntity(saved);
    }

    public BusinessProfileResponse getBusinessProfile(Long userId) {
        log.info("Fetching business profile for user ID: {}", userId);

        BusinessProfile profile = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", userId));

        log.info("Found business profile: {}", profile.getBusinessName());
        return BusinessProfileResponse.fromEntity(profile);
    }

    public BusinessProfileResponse getMyBusinessProfile() {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching business profile for current user: {}", currentUser.getUsername());
        return getBusinessProfile(currentUser.getId());
    }

    @Transactional
    public BusinessProfileResponse updateBusinessProfile(BusinessProfileRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("Updating business profile for user: {}", currentUser.getUsername());

        BusinessProfile profile = businessProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", currentUser.getId()));

        profile.setBusinessName(request.getBusinessName());
        profile.setCategory(request.getCategory());
        profile.setDescription(request.getDescription());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setContactEmail(request.getContactEmail());
        profile.setContactPhone(request.getContactPhone());
        profile.setAddress(request.getAddress());
        profile.setLogoUrl(request.getLogoUrl());
        profile.setCoverImageUrl(request.getCoverImageUrl());

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Business profile updated: {}", saved.getBusinessName());
        return BusinessProfileResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteBusinessProfile() {
        User currentUser = authService.getCurrentUser();
        log.info("Deleting business profile for user: {}", currentUser.getUsername());

        BusinessProfile profile = businessProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", currentUser.getId()));

        businessProfileRepository.delete(profile);
        log.info("Business profile deleted for user: {}", currentUser.getUsername());
    }

    public PagedResponse<BusinessProfileResponse> getBusinessesByCategory(BusinessCategory category, int page,
                                                                          int size) {
        log.info("Fetching businesses in category: {}", category);

        Page<BusinessProfile> profiles = businessProfileRepository.findByCategory(category, PageRequest.of(page, size));

        log.info("Found {} businesses in category {}", profiles.getTotalElements(), category);
        return PagedResponse.fromEntityPage(profiles, BusinessProfileResponse::fromEntity);
    }

    public PagedResponse<BusinessProfileResponse> searchBusinesses(String query, int page, int size) {
        log.info("Searching businesses with query: {}", query);

        Page<BusinessProfile> profiles = businessProfileRepository.findByBusinessNameContainingIgnoreCase(
                query, PageRequest.of(page, size));

        log.info("Found {} businesses matching '{}'", profiles.getTotalElements(), query);
        return PagedResponse.fromEntityPage(profiles, BusinessProfileResponse::fromEntity);
    }

    public AnalyticsResponse getAnalytics(int days) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching analytics for user: {} for last {} days", currentUser.getUsername(), days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // Get daily analytics
        List<PostAnalytics> dailyData = postAnalyticsRepository.findByUserIdAndDateRange(
                currentUser.getId(), startDate, endDate);

        // Calculate totals
        Long totalViews = postAnalyticsRepository.getTotalViewsByUser(currentUser.getId());
        Long totalImpressions = postAnalyticsRepository.getTotalImpressionsByUser(currentUser.getId());
        long totalFollowers = connectionRepository.countByFollowingIdAndStatus(currentUser.getId(),
                ConnectionStatus.ACCEPTED);
        long totalPosts = postRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(0, 1))
                .getTotalElements();

        // Calculate engagement metrics from daily data
        int totalLikes = dailyData.stream().mapToInt(PostAnalytics::getLikes).sum();
        int totalComments = dailyData.stream().mapToInt(PostAnalytics::getComments).sum();
        int totalShares = dailyData.stream().mapToInt(PostAnalytics::getShares).sum();

        // Calculate engagement rate
        double engagementRate = 0.0;
        if (totalImpressions != null && totalImpressions > 0) {
            engagementRate = ((double) (totalLikes + totalComments + totalShares) / totalImpressions) * 100;
        }

        List<AnalyticsResponse.DailyAnalytics> daily = dailyData.stream()
                .map(pa -> AnalyticsResponse.DailyAnalytics.builder()
                        .date(pa.getDate())
                        .views(pa.getViews())
                        .likes(pa.getLikes())
                        .comments(pa.getComments())
                        .shares(pa.getShares())
                        .impressions(pa.getImpressions())
                        .build())
                .collect(Collectors.toList());

        log.info("Analytics retrieved for user {}: {} views, {} followers",
                currentUser.getUsername(), totalViews, totalFollowers);

        return AnalyticsResponse.builder()
                .totalViews(totalViews != null ? totalViews : 0L)
                .totalLikes((long) totalLikes)
                .totalComments((long) totalComments)
                .totalShares((long) totalShares)
                .totalImpressions(totalImpressions != null ? totalImpressions : 0L)
                .totalFollowers(totalFollowers)
                .totalPosts(totalPosts)
                .engagementRate(Math.round(engagementRate * 100.0) / 100.0)
                .dailyData(daily)
                .build();
    }

    @Transactional
    public void recordPostView(Long postId) {
        log.debug("Recording view for post: {}", postId);
        updateAnalytics(postId, pa -> pa.setViews(pa.getViews() + 1));
    }

    @Transactional
    public void recordPostImpression(Long postId) {
        log.debug("Recording impression for post: {}", postId);
        updateAnalytics(postId, pa -> pa.setImpressions(pa.getImpressions() + 1));
    }

    private void updateAnalytics(Long postId, java.util.function.Consumer<PostAnalytics> updater) {
        LocalDate today = LocalDate.now();
        PostAnalytics analytics = postAnalyticsRepository.findByPostIdAndDate(postId, today)
                .orElseGet(() -> {
                    var post = postRepository.findById(postId).orElse(null);
                    if (post == null)
                        return null;
                    return PostAnalytics.builder()
                            .post(post)
                            .date(today)
                            .build();
                });

        if (analytics != null) {
            updater.accept(analytics);
            postAnalyticsRepository.save(analytics);
        }
    }
}
