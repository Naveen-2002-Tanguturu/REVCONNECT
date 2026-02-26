package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.CommentRequest;
import org.revature.revconnect.dto.request.ShareRequest;
import org.revature.revconnect.dto.response.CommentResponse;
import org.revature.revconnect.dto.response.LikeResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.ShareResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.Comment;
import org.revature.revconnect.model.Like;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.Share;
import org.revature.revconnect.model.User;
import org.revature.revconnect.mapper.LikeMapper;
import org.revature.revconnect.mapper.CommentMapper;
import org.revature.revconnect.mapper.ShareMapper;
import org.revature.revconnect.repository.CommentRepository;
import org.revature.revconnect.repository.LikeRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.ShareRepository;
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
class InteractionServiceTest {

    @Mock
    private LikeRepository likeRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ShareRepository shareRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;

    @Mock
    private LikeMapper likeMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private ShareMapper shareMapper;

    @InjectMocks
    private InteractionService interactionService;

    private User testUser;
    private User otherUser;
    private Post testPost;
    private Like testLike;
    private Comment testComment;
    private Share testShare;

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

        testPost = Post.builder()
                .id(1L)
                .content("Test post")
                .user(testUser)
                .likeCount(0)
                .commentCount(0)
                .shareCount(0)
                .build();

        testLike = Like.builder()
                .id(1L)
                .user(testUser)
                .post(testPost)
                .createdAt(LocalDateTime.now())
                .build();

        testComment = Comment.builder()
                .id(1L)
                .content("Test comment")
                .user(testUser)
                .post(testPost)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testShare = Share.builder()
                .id(1L)
                .user(testUser)
                .post(testPost)
                .comment("Sharing this!")
                .createdAt(LocalDateTime.now())
                .build();

        // Common mapper stubbing
        lenient().when(likeMapper.toResponse(any(Like.class))).thenAnswer(invocation -> {
            Like like = invocation.getArgument(0);
            return LikeResponse.builder().id(like.getId()).build();
        });

        lenient().when(commentMapper.toResponse(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            return CommentResponse.builder().id(comment.getId()).content(comment.getContent()).build();
        });

        lenient().when(shareMapper.toResponse(any(Share.class))).thenAnswer(invocation -> {
            Share share = invocation.getArgument(0);
            return ShareResponse.builder().id(share.getId()).build();
        });
    }

    // ==================== LIKE TESTS ====================

    @Test
    void likePost_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(likeRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(false);
        when(likeRepository.save(any(Like.class))).thenReturn(testLike);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        assertDoesNotThrow(() -> interactionService.likePost(1L));
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void likePost_AlreadyLiked() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(likeRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> interactionService.likePost(1L));
    }

    @Test
    void unlikePost_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(likeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.of(testLike));

        assertDoesNotThrow(() -> interactionService.unlikePost(1L));
        verify(likeRepository).delete(testLike);
    }

    @Test
    void unlikePost_NotLiked() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(likeRepository.findByUserIdAndPostId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> interactionService.unlikePost(1L));
    }

    @Test
    void getPostLikes_Success() {
        Page<Like> page = new PageImpl<>(List.of(testLike));
        when(postRepository.existsById(1L)).thenReturn(true);
        when(likeRepository.findByPostIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class))).thenReturn(page);

        PagedResponse<LikeResponse> response = interactionService.getPostLikes(1L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    // ==================== COMMENT TESTS ====================

    @Test
    void addComment_Success() {
        CommentRequest request = CommentRequest.builder().content("New comment").build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        CommentResponse response = interactionService.addComment(1L, request);

        assertNotNull(response);
        assertEquals(testComment.getId(), response.getId());
    }

    @Test
    void getPostComments_Success() {
        Page<Comment> page = new PageImpl<>(List.of(testComment));
        when(postRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class))).thenReturn(page);

        PagedResponse<CommentResponse> response = interactionService.getPostComments(1L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void deleteComment_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        assertDoesNotThrow(() -> interactionService.deleteComment(1L));
        verify(commentRepository).delete(testComment);
    }

    @Test
    void deleteComment_Unauthorized() {
        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        assertThrows(UnauthorizedException.class, () -> interactionService.deleteComment(1L));
    }

    // ==================== SHARE TESTS ====================

    @Test
    void sharePost_Success() {
        ShareRequest request = ShareRequest.builder().comment("Check this out!").build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(shareRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(false);
        when(shareRepository.save(any(Share.class))).thenReturn(testShare);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        ShareResponse response = interactionService.sharePost(1L, request);

        assertNotNull(response);
        assertEquals(testShare.getId(), response.getId());
    }

    @Test
    void sharePost_AlreadyShared() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(shareRepository.existsByUserIdAndPostId(1L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> interactionService.sharePost(1L, null));
    }
}
