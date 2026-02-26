package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.PostRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final AuthService authService;
    private final PostMapper postMapper;

    @Transactional
    public PostResponse createPost(PostRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("Creating post for user: {}", currentUser.getUsername());

        Post post = Post.builder()
                .content(request.getContent())
                .user(currentUser)
                .postType(request.getPostType() != null ? request.getPostType() : PostType.TEXT)
                .mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : new ArrayList<>())
                .build();

        Post savedPost = postRepository.save(post);
        log.info("Post created with ID: {}", savedPost.getId());
        return postMapper.toResponse(savedPost);
    }

    public PostResponse getPostById(Long postId) {
        log.info("Fetching post with ID: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        return postMapper.toResponse(post);
    }

    public PagedResponse<PostResponse> getMyPosts(int page, int size) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching posts for user: {}", currentUser.getUsername());
        Page<Post> posts = postRepository.findByUserIdWithPinnedFirst(currentUser.getId(), PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, postMapper::toResponse);
    }

    public PagedResponse<PostResponse> getUserPosts(Long userId, int page, int size) {
        log.info("Fetching posts for user ID: {}", userId);
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, postMapper::toResponse);
    }

    public PagedResponse<PostResponse> getPublicFeed(int page, int size) {
        log.info("Fetching public feed, page: {}, size: {}", page, size);
        Page<Post> posts = postRepository.findPublicPosts(PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, postMapper::toResponse);
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to edit post {} owned by user {}",
                    currentUser.getId(), postId, post.getUser().getId());
            throw new UnauthorizedException("You can only edit your own posts");
        }

        log.info("Updating post ID: {}", postId);
        post.setContent(request.getContent());
        if (request.getPostType() != null) {
            post.setPostType(request.getPostType());
        }
        if (request.getMediaUrls() != null) {
            post.setMediaUrls(request.getMediaUrls());
        }

        Post updatedPost = postRepository.save(post);
        log.info("Post updated successfully: {}", postId);
        return postMapper.toResponse(updatedPost);
    }

    @Transactional
    public void deletePost(Long postId) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to delete post {} owned by user {}",
                    currentUser.getId(), postId, post.getUser().getId());
            throw new UnauthorizedException("You can only delete your own posts");
        }

        log.info("Deleting post ID: {}", postId);
        postRepository.delete(post);
        log.info("Post deleted successfully: {}", postId);
    }

    @Transactional
    public PostResponse togglePinPost(Long postId) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only pin your own posts");
        }

        post.setPinned(!post.getPinned());
        Post updatedPost = postRepository.save(post);
        log.info("Post {} pinned status changed to: {}", postId, updatedPost.getPinned());
        return postMapper.toResponse(updatedPost);
    }
}
