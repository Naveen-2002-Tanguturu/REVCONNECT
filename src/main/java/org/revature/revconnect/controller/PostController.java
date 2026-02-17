package org.revature.revconnect.controller;
import org.revature.revconnect.dto.request.PostRequest;
import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "Post management APIs")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Create a new post")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody PostRequest request) {
        log.info("Create post request received");
        PostResponse post = postService.createPost(request);
        log.info("Post created successfully with ID: {}", post.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", post));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get a post by ID")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long postId) {
        log.info("Get post request for ID: {}", postId);
        PostResponse post = postService.getPostById(postId);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my posts")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get my posts request - page: {}, size: {}", page, size);
        PagedResponse<PostResponse> posts = postService.getMyPosts(page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user ID")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get posts for user ID: {}", userId);
        PagedResponse<PostResponse> posts = postService.getUserPosts(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/feed")
    @Operation(summary = "Get public feed")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get public feed request - page: {}, size: {}", page, size);
        PagedResponse<PostResponse> posts = postService.getPublicFeed(page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update a post")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostRequest request) {
        log.info("Update post request for ID: {}", postId);
        PostResponse post = postService.updatePost(postId, request);
        log.info("Post updated successfully: {}", postId);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", post));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete a post")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
        log.info("Delete post request for ID: {}", postId);
        postService.deletePost(postId);
        log.info("Post deleted successfully: {}", postId);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    @PatchMapping("/{postId}/pin")
    @Operation(summary = "Toggle pin status of a post")
    public ResponseEntity<ApiResponse<PostResponse>> togglePinPost(@PathVariable Long postId) {
        log.info("Toggle pin request for post ID: {}", postId);
        PostResponse post = postService.togglePinPost(postId);
        String message = post.getPinned() ? "Post pinned" : "Post unpinned";
        return ResponseEntity.ok(ApiResponse.success(message, post));
    }
}
