package org.revature.revconnect.service;

import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Story;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private StoryService storyService;

    private User testUser;
    private User otherUser;
    private Story testStory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .name("Other User")
                .build();

        testStory = Story.builder()
                .id(1L)
                .user(testUser)
                .mediaUrl("http://example.com/image.jpg")
                .caption("Test story")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .isHighlight(false)
                .viewCount(0)
                .build();
    }

    @Test
    void createStory_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(storyRepository.save(any(Story.class))).thenReturn(testStory);

        Story result = storyService.createStory("http://example.com/image.jpg", "Test story");

        assertNotNull(result);
        assertEquals(testStory.getId(), result.getId());
        assertEquals("Test story", result.getCaption());
        verify(storyRepository).save(any(Story.class));
    }

    @Test
    void getActiveStories_Success() {
        when(storyRepository.findByUserAndExpiresAtAfterOrderByCreatedAtDesc(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of(testStory));

        List<Story> stories = storyService.getActiveStories(testUser);

        assertNotNull(stories);
        assertEquals(1, stories.size());
    }

    @Test
    void getStoriesFeed_Success() {
        List<User> followedUsers = List.of(otherUser);
        when(storyRepository.findActiveStoriesByUsers(eq(followedUsers), any(LocalDateTime.class)))
                .thenReturn(List.of(testStory));

        List<Story> stories = storyService.getStoriesFeed(followedUsers);

        assertNotNull(stories);
        assertEquals(1, stories.size());
    }

    @Test
    void getHighlights_Success() {
        testStory.setHighlight(true);
        when(storyRepository.findByUserAndIsHighlightTrueOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(testStory));

        List<Story> highlights = storyService.getHighlights(testUser);

        assertNotNull(highlights);
        assertEquals(1, highlights.size());
    }

    @Test
    void markAsHighlight_Success() {
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        when(storyRepository.save(any(Story.class))).thenReturn(testStory);

        Story result = storyService.markAsHighlight(1L);

        assertNotNull(result);
        verify(storyRepository).save(any(Story.class));
    }

    @Test
    void markAsHighlight_NotFound() {
        when(storyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> storyService.markAsHighlight(99L));
    }

    @Test
    void incrementViewCount_Success() {
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        when(storyRepository.save(any(Story.class))).thenReturn(testStory);

        storyService.incrementViewCount(1L);

        assertEquals(1, testStory.getViewCount());
        verify(storyRepository).save(testStory);
    }

    @Test
    void deleteStory_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));

        storyService.deleteStory(1L);

        verify(storyRepository).delete(testStory);
    }

    @Test
    void deleteStory_Unauthorized() {
        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));

        assertThrows(RuntimeException.class, () -> storyService.deleteStory(1L));
    }

    @Test
    void cleanupExpiredStories_Success() {
        when(storyRepository.findExpiredStories(any(LocalDateTime.class)))
                .thenReturn(List.of(testStory));

        storyService.cleanupExpiredStories();

        verify(storyRepository).deleteAll(List.of(testStory));
    }
}
