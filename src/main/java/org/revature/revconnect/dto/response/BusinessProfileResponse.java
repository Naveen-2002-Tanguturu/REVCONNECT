package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.BusinessCategory;
import org.revature.revconnect.model.BusinessProfile;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfileResponse {

    private Long id;
    private Long userId;
    private String username;
    private String businessName;
    private BusinessCategory category;
    private String description;
    private String websiteUrl;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String logoUrl;
    private String coverImageUrl;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BusinessProfileResponse fromEntity(BusinessProfile profile) {
        return BusinessProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .username(profile.getUser().getUsername())
                .businessName(profile.getBusinessName())
                .category(profile.getCategory())
                .description(profile.getDescription())
                .websiteUrl(profile.getWebsiteUrl())
                .contactEmail(profile.getContactEmail())
                .contactPhone(profile.getContactPhone())
                .address(profile.getAddress())
                .logoUrl(profile.getLogoUrl())
                .coverImageUrl(profile.getCoverImageUrl())
                .isVerified(profile.getIsVerified())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
