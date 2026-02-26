package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.ProfileUpdateRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.User;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .userType(UserType.PERSONAL)
                .privacy(Privacy.PUBLIC)
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .name("Other User")
                .userType(UserType.PERSONAL)
                .privacy(Privacy.PUBLIC)
                .build();

        // Common mapper stubbing
        lenient().when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
        });

        lenient().when(userMapper.toPublicResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .build();
        });

        userService = new UserService(userRepository, authService, userMapper);
    }

    @Test
    @DisplayName("Should get current user profile")
    void getMyProfile_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);

        // Act
        UserResponse response = userService.getMyProfile();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should get user by ID")
    void getUserById_Success() {

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(authService.getCurrentUser()).thenReturn(testUser);

        UserResponse response = userService.getUserById(2L);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("otheruser");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found by ID")
    void getUserById_NotFound() {

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for private profile")
    void getUserById_PrivateProfile() {

        otherUser.setPrivacy(Privacy.PRIVATE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(authService.getCurrentUser()).thenReturn(testUser);


        assertThatThrownBy(() -> userService.getUserById(2L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("private");
    }

    @Test
    @DisplayName("Should update profile successfully")
    void updateProfile_Success() {

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .name("Updated Name")
                .bio("New bio")
                .location("New York")
                .build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);


        UserResponse response = userService.updateProfile(request);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should search users successfully")
    void searchUsers_Success() {
        // Arrange
        Page<User> usersPage = new PageImpl<>(List.of(testUser, otherUser));
        when(userRepository.searchPublicUsers(anyString(), any(PageRequest.class)))
                .thenReturn(usersPage);

        // Act
        PagedResponse<UserResponse> response = userService.searchUsers("test", 0, 10);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should update privacy settings")
    void updatePrivacy_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse response = userService.updatePrivacy(Privacy.PRIVATE);

        // Assert
        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }



    @Test
    @DisplayName("Should throw BadRequestException when blocking self")
    void blockUser_Self_ThrowsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        assertThatThrownBy(() -> userService.blockUser(1L)) // same ID as testUser
                .isInstanceOf(org.revature.revconnect.exception.BadRequestException.class)
                .hasMessageContaining("Cannot block yourself");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when blocking non-existent user")
    void blockUser_UserNotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.blockUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("Should block user successfully")
    void blockUser_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));


        userService.blockUser(2L);

        verify(userRepository).findById(2L);
    }

    // ======================== UNBLOCK USER ========================

    @Test
    @DisplayName("Should throw ResourceNotFoundException when unblocking non-existent user")
    void unblockUser_UserNotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.unblockUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should unblock user successfully")
    void unblockUser_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        userService.unblockUser(2L);

        verify(userRepository).findById(2L);
    }

    @Test
    @DisplayName("Should throw BadRequestException when reporting self")
    void reportUser_Self_ThrowsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        assertThatThrownBy(() -> userService.reportUser(1L, "spam"))
                .isInstanceOf(org.revature.revconnect.exception.BadRequestException.class)
                .hasMessageContaining("Cannot report yourself");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reporting non-existent user")
    void reportUser_UserNotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.reportUser(999L, "spam"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should report user successfully")
    void reportUser_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        userService.reportUser(2L, "inappropriate content");

        verify(userRepository).findById(2L);
    }
    @Test
    @DisplayName("Should return empty blocked users list")
    void getBlockedUsers_ReturnsEmpty() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        PagedResponse<UserResponse> response = userService.getBlockedUsers(0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should update profile with all null fields — no fields changed")
    void updateProfile_AllNullFields_SavesUnchangedUser() {
        ProfileUpdateRequest emptyRequest = ProfileUpdateRequest.builder().build(); // all null

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.updateProfile(emptyRequest);

        assertThat(response).isNotNull();
        // name/bio/location/etc. remain original values
        assertThat(testUser.getName()).isEqualTo("Test User");
        verify(userRepository).save(testUser);
    }
}
