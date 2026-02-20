
package org.revature.revconnect.controller;
import org.revature.revconnect.dto.request.CommentRequest;
import org.revature.revconnect.dto.request.ShareRequest;
import org.revature.revconnect.dto.response.*;
import org.revature.revconnect.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Interactions", description = "Like, Comment, Share APIs")
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "Like a post")
    public ResponseEntity<ApiResponse<Void>> likePost(@PathVariable Long postId) {
        log.info("Like post request for post ID: {}", postId);
        interactionService.likePost(postId);
        log.info("Post {} liked successfully", postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post liked successfully", null));
    }

    @DeleteMapping("/posts/{postId}/like")
    @Operation(summary = "Unlike a post")
    public ResponseEntity<ApiResponse<Void>> unlikePost(@PathVariable Long postId) {
        log.info("Unlike post request for post ID: {}", postId);
        interactionService.unlikePost(postId);
        log.info("Post {} unliked successfully", postId);
        return ResponseEntity.ok(ApiResponse.success("Post unliked successfully", null));
    }

    @GetMapping("/posts/{postId}/likes")
    @Operation(summary = "Get users who liked a post")
    public ResponseEntity<ApiResponse<PagedResponse<LikeResponse>>> getPostLikes(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get likes request for post ID: {}", postId);
        PagedResponse<LikeResponse> likes = interactionService.getPostLikes(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(likes));
    }

    @GetMapping("/posts/{postId}/liked")
    @Operation(summary = "Check if current user has liked a post")
    public ResponseEntity<ApiResponse<Boolean>> hasUserLikedPost(@PathVariable Long postId) {
        log.info("Check like status for post ID: {}", postId);
        boolean liked = interactionService.hasUserLikedPost(postId);
        return ResponseEntity.ok(ApiResponse.success(liked));
    }
    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Add a comment to a post")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {
        log.info("Add comment request for post ID: {}", postId);
        CommentResponse comment = interactionService.addComment(postId, request);
        log.info("Comment {} added to post {} successfully", comment.getId(), postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", comment));
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comments on a post")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get comments request for post ID: {}", postId);
        PagedResponse<CommentResponse> comments = interactionService.getPostComments(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        log.info("Delete comment request for comment ID: {}", commentId);
        interactionService.deleteComment(commentId);
        log.info("Comment {} deleted successfully", commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }

    @PostMapping("/posts/{postId}/share")
    @Operation(summary = "Share/Repost a post")
    public ResponseEntity<ApiResponse<ShareResponse>> sharePost(
            @PathVariable Long postId,
            @RequestBody(required = false) ShareRequest request) {
        log.info("Share post request for post ID: {}", postId);
        ShareResponse share = interactionService.sharePost(postId, request);
        log.info("Post {} shared successfully", postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post shared successfully", share));
    }
}
