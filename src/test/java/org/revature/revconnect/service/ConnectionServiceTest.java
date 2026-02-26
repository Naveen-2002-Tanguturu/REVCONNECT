package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.ConnectionResponse;
import org.revature.revconnect.dto.response.ConnectionStatsResponse;
import org.revature.revconnect.dto.response.PagedResponse;

import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Connection;
import org.revature.revconnect.mapper.ConnectionMapper;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;

    @Mock
    private ConnectionMapper connectionMapper;

    @InjectMocks
    private ConnectionService connectionService;

    private User testUser;
    private User targetUser;
    private Connection testConnection;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .build();

        targetUser = User.builder()
                .id(2L)
                .username("targetuser")
                .email("target@example.com")
                .name("Target User")
                .build();

        testConnection = Connection.builder()
                .id(1L)
                .follower(testUser)
                .following(targetUser)
                .status(ConnectionStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .build();

        // Common mapper stubbing
        lenient().when(connectionMapper.fromFollower(any(Connection.class))).thenAnswer(invocation -> {
            Connection c = invocation.getArgument(0);
            return ConnectionResponse.builder().id(c.getId()).build();
        });

        lenient().when(connectionMapper.fromFollowing(any(Connection.class))).thenAnswer(invocation -> {
            Connection c = invocation.getArgument(0);
            return ConnectionResponse.builder().id(c.getId()).build();
        });
    }


    @Test
    void followUser_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(connectionRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
        when(connectionRepository.save(any(Connection.class))).thenReturn(testConnection);

        assertDoesNotThrow(() -> connectionService.followUser(2L));
        verify(connectionRepository).save(any(Connection.class));
    }

    @Test
    void followUser_CannotFollowSelf() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        assertThrows(BadRequestException.class, () -> connectionService.followUser(1L));
    }

    @Test
    void followUser_AlreadyFollowing() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(connectionRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> connectionService.followUser(2L));
    }

    @Test
    void followUser_UserNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> connectionService.followUser(2L));
    }


    @Test
    void unfollowUser_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(connectionRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(testConnection));

        assertDoesNotThrow(() -> connectionService.unfollowUser(2L));
        verify(connectionRepository).delete(testConnection);
    }

    @Test
    void unfollowUser_NotFollowing() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(connectionRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> connectionService.unfollowUser(2L));
    }


    @Test
    void getFollowers_Success() {
        Page<Connection> page = new PageImpl<>(List.of(testConnection));
        when(userRepository.existsById(2L)).thenReturn(true);
        when(connectionRepository.findFollowersByUserId(eq(2L), eq(ConnectionStatus.ACCEPTED), any(PageRequest.class)))
                .thenReturn(page);

        PagedResponse<ConnectionResponse> response = connectionService.getFollowers(2L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getFollowing_Success() {
        Page<Connection> page = new PageImpl<>(List.of(testConnection));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(connectionRepository.findFollowingByUserId(eq(1L), eq(ConnectionStatus.ACCEPTED), any(PageRequest.class)))
                .thenReturn(page);

        PagedResponse<ConnectionResponse> response = connectionService.getFollowing(1L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getFollowers_UserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> connectionService.getFollowers(999L, 0, 10));
    }


    @Test
    void getConnectionStats_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(connectionRepository.countByFollowingIdAndStatus(2L, ConnectionStatus.ACCEPTED)).thenReturn(100L);
        when(connectionRepository.countByFollowerIdAndStatus(2L, ConnectionStatus.ACCEPTED)).thenReturn(50L);
        when(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(1L, 2L, ConnectionStatus.ACCEPTED))
                .thenReturn(true);
        when(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(2L, 1L, ConnectionStatus.ACCEPTED))
                .thenReturn(false);

        ConnectionStatsResponse stats = connectionService.getConnectionStats(2L);

        assertNotNull(stats);
        assertEquals(100L, stats.getFollowersCount());
        assertEquals(50L, stats.getFollowingCount());
        assertTrue(stats.isFollowing());
        assertFalse(stats.isFollowedBy());
    }


    @Test
    void acceptRequest_Success() {
        Connection pendingConnection = Connection.builder()
                .id(1L)
                .follower(targetUser)
                .following(testUser)
                .status(ConnectionStatus.PENDING)
                .build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(pendingConnection));
        when(connectionRepository.save(any(Connection.class))).thenReturn(pendingConnection);

        assertDoesNotThrow(() -> connectionService.acceptRequest(1L));
        assertEquals(ConnectionStatus.ACCEPTED, pendingConnection.getStatus());
    }

    @Test
    void rejectRequest_Success() {
        Connection pendingConnection = Connection.builder()
                .id(1L)
                .follower(targetUser)
                .following(testUser)
                .status(ConnectionStatus.PENDING)
                .build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(pendingConnection));

        assertDoesNotThrow(() -> connectionService.rejectRequest(1L));
        verify(connectionRepository).delete(pendingConnection);
    }
}
