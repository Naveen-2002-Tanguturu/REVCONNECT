package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.NotificationResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.enums.NotificationType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.Notification;
import org.revature.revconnect.mapper.NotificationMapper;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.NotificationRepository;
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
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuthService authService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private User actorUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .build();

        actorUser = User.builder()
                .id(2L)
                .username("actoruser")
                .email("actor@example.com")
                .name("Actor User")
                .build();

        testNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .actor(actorUser)
                .type(NotificationType.LIKE)
                .message("Actor User liked your post")
                .referenceId(10L)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Common mapper stubbing
        lenient().when(notificationMapper.toResponse(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            return NotificationResponse.builder().id(n.getId()).message(n.getMessage()).build();
        });
    }

    @Test
    void createNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        assertDoesNotThrow(() -> notificationService.createNotification(
                testUser, actorUser, NotificationType.LIKE, "Liked your post", 10L));

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_SkipsSelfNotification() {
        notificationService.createNotification(testUser, testUser, NotificationType.LIKE, "Message", 10L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void getNotifications_Success() {
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class))).thenReturn(page);

        PagedResponse<NotificationResponse> response = notificationService.getNotifications(0, 20);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getUnreadNotifications_Success() {
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        PagedResponse<NotificationResponse> response = notificationService.getUnreadNotifications(0, 20);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void getUnreadCount_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount();

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        assertDoesNotThrow(() -> notificationService.markAsRead(1L));
        assertTrue(testNotification.getIsRead());
    }

    @Test
    void markAsRead_NotificationNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(999L));
    }

    @Test
    void markAsRead_Unauthorized() {
        when(authService.getCurrentUser()).thenReturn(actorUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        assertThrows(UnauthorizedException.class, () -> notificationService.markAsRead(1L));
    }

    @Test
    void markAllAsRead_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.markAllAsRead(1L)).thenReturn(3);

        int count = notificationService.markAllAsRead();

        assertEquals(3, count);
    }

    @Test
    void deleteNotification_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        assertDoesNotThrow(() -> notificationService.deleteNotification(1L));
        verify(notificationRepository).delete(testNotification);
    }

    @Test
    void deleteNotification_Unauthorized() {
        when(authService.getCurrentUser()).thenReturn(actorUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        assertThrows(UnauthorizedException.class, () -> notificationService.deleteNotification(1L));
    }

    @Test
    void notifyLike_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        assertDoesNotThrow(() -> notificationService.notifyLike(testUser, actorUser, 10L));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void notifyFollow_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        assertDoesNotThrow(() -> notificationService.notifyFollow(testUser, actorUser));
        verify(notificationRepository).save(any(Notification.class));
    }
}
