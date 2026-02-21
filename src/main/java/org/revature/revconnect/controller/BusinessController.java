package org.revature.revconnect.controller;

import org.revature.revconnect.dto.request.BusinessProfileRequest;
import org.revature.revconnect.dto.response.*;
import org.revature.revconnect.enums.BusinessCategory;
import org.revature.revconnect.service.BusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Business", description = "Business Profile and Analytics APIs")
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping("/profile")
    @Operation(summary = "Create a business profile")
    public ResponseEntity<ApiResponse<BusinessProfileResponse>> createProfile(
            @Valid @RequestBody BusinessProfileRequest request) {
        log.info("Create business profile request");
        BusinessProfileResponse profile = businessService.createBusinessProfile(request);
        log.info("Business profile created: {}", profile.getBusinessName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Business profile created successfully", profile));
    }

    @GetMapping("/profile/me")
    @Operation(summary = "Get my business profile")
    public ResponseEntity<ApiResponse<BusinessProfileResponse>> getMyProfile() {
        log.info("Get my business profile request");
        BusinessProfileResponse profile = businessService.getMyBusinessProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get business profile by user ID")
    public ResponseEntity<ApiResponse<BusinessProfileResponse>> getProfile(@PathVariable Long userId) {
        log.info("Get business profile for user ID: {}", userId);
        BusinessProfileResponse profile = businessService.getBusinessProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my business profile")
    public ResponseEntity<ApiResponse<BusinessProfileResponse>> updateProfile(
            @Valid @RequestBody BusinessProfileRequest request) {
        log.info("Update business profile request");
        BusinessProfileResponse profile = businessService.updateBusinessProfile(request);
        log.info("Business profile updated: {}", profile.getBusinessName());
        return ResponseEntity.ok(ApiResponse.success("Business profile updated successfully", profile));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete my business profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile() {
        log.info("Delete business profile request");
        businessService.deleteBusinessProfile();
        log.info("Business profile deleted successfully");
        return ResponseEntity.ok(ApiResponse.success("Business profile deleted successfully", null));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get businesses by category")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessProfileResponse>>> getByCategory(
            @PathVariable BusinessCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get businesses by category: {}", category);
        PagedResponse<BusinessProfileResponse> businesses = businessService.getBusinessesByCategory(category, page,
                size);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    @GetMapping("/search")
    @Operation(summary = "Search businesses by name")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessProfileResponse>>> searchBusinesses(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Search businesses with query: {}", query);
        PagedResponse<BusinessProfileResponse> businesses = businessService.searchBusinesses(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get analytics for current user")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Get analytics for last {} days", days);
        AnalyticsResponse analytics = businessService.getAnalytics(days);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @PostMapping("/posts/{postId}/view")
    @Operation(summary = "Record a post view")
    public ResponseEntity<ApiResponse<Void>> recordView(@PathVariable Long postId) {
        log.debug("Recording view for post: {}", postId);
        businessService.recordPostView(postId);
        return ResponseEntity.ok(ApiResponse.success("View recorded", null));
    }

    @PostMapping("/posts/{postId}/impression")
    @Operation(summary = "Record a post impression")
    public ResponseEntity<ApiResponse<Void>> recordImpression(@PathVariable Long postId) {
        log.debug("Recording impression for post: {}", postId);
        businessService.recordPostImpression(postId);
        return ResponseEntity.ok(ApiResponse.success("Impression recorded", null));
    }
}
