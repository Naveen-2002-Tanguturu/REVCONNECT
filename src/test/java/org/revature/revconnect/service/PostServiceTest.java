package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.PostRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.PostRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private AuthService authService;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private User otherUser;
    private Post testPost;

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
                .content("Test post content")
                .user(testUser)
                .postType(PostType.TEXT)
                .mediaUrls(new ArrayList<>())
                .pinned(false)
                .likeCount(0)
                .commentCount(0)
                .shareCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Common mapper stubbing
        lenient().when(postMapper.toResponse(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            return PostResponse.builder()
                    .id(post.getId())
                    .content(post.getContent())
                    .postType(post.getPostType())
                    .build();
        });
    }

    @Test
    void createPost_Success() {
        PostRequest request = PostRequest.builder()
                .content("New post content")
                .postType(PostType.TEXT)
                .build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse response = postService.createPost(request);

        assertNotNull(response);
        assertEquals(testPost.getId(), response.getId());
        assertEquals(testPost.getContent(), response.getContent());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void getPostById_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        PostResponse response = postService.getPostById(1L);

        assertNotNull(response);
        assertEquals(testPost.getId(), response.getId());
        assertEquals(testPost.getContent(), response.getContent());
    }

    @Test
    void getPostById_NotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(99L));
    }

    @Test
    void getMyPosts_Success() {
        Page<Post> page = new PageImpl<>(List.of(testPost));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findByUserIdWithPinnedFirst(eq(1L), any(PageRequest.class))).thenReturn(page);

        PagedResponse<PostResponse> response = postService.getMyPosts(0, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
    }

    @Test
    void updatePost_Success() {
        PostRequest request = PostRequest.builder()
                .content("Updated content")
                .build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse response = postService.updatePost(1L, request);

        assertNotNull(response);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void updatePost_Unauthorized() {
        PostRequest request = PostRequest.builder()
                .content("Updated content")
                .build();

        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertThrows(UnauthorizedException.class, () -> postService.updatePost(1L, request));
    }

    @Test
    void deletePost_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        postService.deletePost(1L);

        verify(postRepository).delete(testPost);
    }

    @Test
    void deletePost_Unauthorized() {
        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        assertThrows(UnauthorizedException.class, () -> postService.deletePost(1L));
    }

    @Test
    void togglePinPost_Success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse response = postService.togglePinPost(1L);

        assertNotNull(response);
        verify(postRepository).save(any(Post.class));
    }
}
