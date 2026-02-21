package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.ProfileUpdateRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserResponse getMyProfile() {
        User currentUser = authService.getCurrentUser();
        return UserResponse.fromEntity(currentUser);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        User currentUser = authService.getCurrentUser();

        if (user.getPrivacy() == Privacy.PRIVATE &&
                !user.getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("This profile is private");
        }

        return UserResponse.fromEntityPublic(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        User currentUser = authService.getCurrentUser();

        if (user.getPrivacy() == Privacy.PRIVATE &&
                !user.getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("This profile is private");
        }

        return UserResponse.fromEntityPublic(user);
    }

    @Transactional
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        User currentUser = authService.getCurrentUser();

        if (request.getName() != null) currentUser.setName(request.getName());
        if (request.getBio() != null) currentUser.setBio(request.getBio());
        if (request.getProfilePicture() != null) currentUser.setProfilePicture(request.getProfilePicture());
        if (request.getLocation() != null) currentUser.setLocation(request.getLocation());
        if (request.getWebsite() != null) currentUser.setWebsite(request.getWebsite());
        if (request.getPrivacy() != null) currentUser.setPrivacy(request.getPrivacy());

        if (request.getBusinessName() != null) currentUser.setBusinessName(request.getBusinessName());
        if (request.getCategory() != null) currentUser.setCategory(request.getCategory());
        if (request.getIndustry() != null) currentUser.setIndustry(request.getIndustry());
        if (request.getContactInfo() != null) currentUser.setContactInfo(request.getContactInfo());
        if (request.getBusinessAddress() != null) currentUser.setBusinessAddress(request.getBusinessAddress());
        if (request.getBusinessHours() != null) currentUser.setBusinessHours(request.getBusinessHours());
        if (request.getExternalLinks() != null) currentUser.setExternalLinks(request.getExternalLinks());
        if (request.getSocialMediaLinks() != null) currentUser.setSocialMediaLinks(request.getSocialMediaLinks());

        return UserResponse.fromEntity(userRepository.save(currentUser));
    }

    public PagedResponse<UserResponse> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage =
                userRepository.searchPublicUsers(query, Privacy.PUBLIC, pageable);

        return PagedResponse.fromEntityPage(usersPage, UserResponse::fromEntityPublic);
    }

    @Transactional
    public UserResponse updatePrivacy(Privacy privacy) {
        User currentUser = authService.getCurrentUser();
        currentUser.setPrivacy(privacy);
        return UserResponse.fromEntity(userRepository.save(currentUser));
    }

    public PagedResponse<UserResponse> getSuggestedUsers(int page, int size) {
        User currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        Page<User> usersPage =
                userRepository.findSuggestedUsers(
                        currentUser.getId(),
                        Privacy.PUBLIC,
                        pageable
                );

        return PagedResponse.fromEntityPage(usersPage, UserResponse::fromEntityPublic);
    }

    @Transactional
    public void blockUser(Long userId) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("Cannot block yourself");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        log.info("User {} blocked user {}", currentUser.getId(), userId);
    }

    @Transactional
    public void unblockUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        log.info("User {} unblocked", userId);
    }

    public PagedResponse<UserResponse> getBlockedUsers(int page, int size) {
        return PagedResponse.<UserResponse>builder()
                .content(List.of())
                .pageNumber(page)
                .pageSize(size)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();
    }

    public PagedResponse<UserResponse> getMutualConnections(Long userId, int page, int size) {
        User currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        Page<User> mutualPage =
                userRepository.findMutualConnections(
                        currentUser.getId(),
                        userId,
                        ConnectionStatus.ACCEPTED,
                        pageable
                );

        return PagedResponse.fromEntityPage(mutualPage, UserResponse::fromEntityPublic);
    }

    @Transactional
    public void reportUser(Long userId, String reason) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("You cannot report yourself");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        log.info("User {} reported user {} | Reason: {}",
                currentUser.getId(), userId, reason);
    }
}