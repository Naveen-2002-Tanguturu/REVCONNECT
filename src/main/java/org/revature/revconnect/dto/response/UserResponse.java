package org.revature.revconnect.dto.response;


import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String name;
    private UserType userType;
    private String bio;
    private String profilePicture;
    private String location;
    private String website;
    private Privacy privacy;
    private Boolean isVerified;

    // Business/Creator fields
    private String businessName;
    private String category;
    private String industry;
    private String contactInfo;
    private String businessAddress;
    private String businessHours;
    private String externalLinks;
    private String socialMediaLinks;

    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .bio(user.getBio())
                .profilePicture(user.getProfilePicture())
                .location(user.getLocation())
                .website(user.getWebsite())
                .privacy(user.getPrivacy())
                .isVerified(user.getIsVerified())
                .businessName(user.getBusinessName())
                .category(user.getCategory())
                .industry(user.getIndustry())
                .contactInfo(user.getContactInfo())
                .businessAddress(user.getBusinessAddress())
                .businessHours(user.getBusinessHours())
                .externalLinks(user.getExternalLinks())
                .socialMediaLinks(user.getSocialMediaLinks())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // For public profile view (hides email for private users)
    public static UserResponse fromEntityPublic(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .userType(user.getUserType())
                .bio(user.getBio())
                .profilePicture(user.getProfilePicture())
                .location(user.getLocation())
                .website(user.getWebsite())
                .privacy(user.getPrivacy())
                .isVerified(user.getIsVerified())
                .businessName(user.getBusinessName())
                .category(user.getCategory())
                .industry(user.getIndustry())
                .contactInfo(user.getContactInfo())
                .businessAddress(user.getBusinessAddress())
                .businessHours(user.getBusinessHours())
                .externalLinks(user.getExternalLinks())
                .socialMediaLinks(user.getSocialMediaLinks())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
