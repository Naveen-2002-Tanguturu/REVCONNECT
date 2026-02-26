package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.BookmarkResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Bookmark;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.BookmarkRepository;
import org.revature.revconnect.repository.PostRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookmarkService bookmarkService;

    private User testUser;
    private Post testPost;
    private Bookmark testBookmark;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .build();

        testPost = Post.builder()
                .id(1L)
                .content("Test post")
                .user(testUser)
                .likeCount(0)
                .commentCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testBookmark = Bookmark.builder()
                .id(1L)
                .user(testUser)
                .post(testPost)
                .createdAt(LocalDateTime.now())
                .build();

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void bookmarkPost_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(bookmarkRepository.existsByUserAndPost(testUser, testPost)).thenReturn(false);
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(testBookmark);

        assertDoesNotThrow(() -> bookmarkService.bookmarkPost(1L));
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void bookmarkPost_AlreadyBookmarked() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(bookmarkRepository.existsByUserAndPost(testUser, testPost)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> bookmarkService.bookmarkPost(1L));
    }

    @Test
    void bookmarkPost_PostNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookmarkService.bookmarkPost(99L));
    }

    @Test
    void removeBookmark_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertDoesNotThrow(() -> bookmarkService.removeBookmark(1L));
        verify(bookmarkRepository).deleteByUserAndPost(testUser, testPost);
    }

    @Test
    void getBookmarks_Success() {
        Page<Bookmark> page = new PageImpl<>(List.of(testBookmark));
        when(bookmarkRepository.findByUserOrderByCreatedAtDesc(eq(testUser), any(PageRequest.class)))
                .thenReturn(page);

        PagedResponse<BookmarkResponse> response = bookmarkService.getBookmarks(0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
    }

    @Test
    void isBookmarked_ReturnsTrue() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(bookmarkRepository.existsByUserAndPost(testUser, testPost)).thenReturn(true);

        assertTrue(bookmarkService.isBookmarked(1L));
    }

    @Test
    void isBookmarked_ReturnsFalse() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(bookmarkRepository.existsByUserAndPost(testUser, testPost)).thenReturn(false);

        assertFalse(bookmarkService.isBookmarked(1L));
    }
}
