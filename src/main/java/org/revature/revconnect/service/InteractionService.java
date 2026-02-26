package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.CommentRequest;
import org.revature.revconnect.dto.request.ShareRequest;
import org.revature.revconnect.dto.response.CommentResponse;
import org.revature.revconnect.dto.response.LikeResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.ShareResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.CommentMapper;
import org.revature.revconnect.mapper.LikeMapper;
import org.revature.revconnect.mapper.ShareMapper;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.Comment;
import org.revature.revconnect.model.Like;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.Share;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.CommentRepository;
import org.revature.revconnect.repository.LikeRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionService {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ShareRepository shareRepository;
    private final PostRepository postRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final LikeMapper likeMapper;
    private final CommentMapper commentMapper;
    private final ShareMapper shareMapper;

    // ==================== LIKE OPERATIONS ====================

    @Transactional
    public void likePost(long postId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to like post {}", currentUser.getUsername(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (likeRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
            log.warn("User {} already liked post {}", currentUser.getUsername(), postId);
            throw new BadRequestException("You have already liked this post");
        }

        Like like = Like.builder()
                .user(currentUser)
                .post(post)
                .build();

        likeRepository.save(like);
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        // Send notification to post owner
        notificationService.notifyLike(post.getUser(), currentUser, postId);

        log.info("User {} successfully liked post {}", currentUser.getUsername(), postId);
    }

    @Transactional
    public void unlikePost(Long postId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to unlike post {}", currentUser.getUsername(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Like like = likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .orElseThrow(() -> new BadRequestException("You have not liked this post"));

        likeRepository.delete(like);
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postRepository.save(post);

        log.info("User {} successfully unliked post {}", currentUser.getUsername(), postId);
    }

    public PagedResponse<LikeResponse> getPostLikes(Long postId, int page, int size) {
        log.info("Fetching likes for post {}, page: {}, size: {}", postId, page, size);

        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        Page<Like> likes = likeRepository.findByPostIdOrderByCreatedAtDesc(postId, PageRequest.of(page, size));
        log.info("Found {} likes for post {}", likes.getTotalElements(), postId);
        return PagedResponse.fromEntityPage(likes, likeMapper::toResponse);
    }

    public boolean hasUserLikedPost(Long postId) {
        User currentUser = authService.getCurrentUser();
        return likeRepository.existsByUserIdAndPostId(currentUser.getId(), postId);
    }


    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} adding comment to post {}", currentUser.getUsername(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(currentUser)
                .post(post)
                .build();

        Comment savedComment = commentRepository.save(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        notificationService.notifyComment(post.getUser(), currentUser, postId);

        log.info("Comment {} added to post {} by user {}", savedComment.getId(), postId, currentUser.getUsername());
        return commentMapper.toResponse(savedComment);
    }

    public PagedResponse<CommentResponse> getPostComments(Long postId, int page, int size) {
        log.info("Fetching comments for post {}, page: {}, size: {}", postId, page, size);

        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        Page<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, PageRequest.of(page, size));
        log.info("Found {} comments for post {}", comments.getTotalElements(), postId);
        return PagedResponse.fromEntityPage(comments, commentMapper::toResponse);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to delete comment {}", currentUser.getUsername(), commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to delete comment {} owned by user {}",
                    currentUser.getId(), commentId, comment.getUser().getId());
            throw new UnauthorizedException("You can only delete your own comments");
        }

        Post post = comment.getPost();
        commentRepository.delete(comment);
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);

        log.info("Comment {} deleted successfully by user {}", commentId, currentUser.getUsername());
    }

    @Transactional
    public ShareResponse sharePost(Long postId, ShareRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to share post {}", currentUser.getUsername(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (shareRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
            log.warn("User {} already shared post {}", currentUser.getUsername(), postId);
            throw new BadRequestException("You have already shared this post");
        }

        Share share = Share.builder()
                .user(currentUser)
                .post(post)
                .comment(request != null ? request.getComment() : null)
                .build();

        Share savedShare = shareRepository.save(share);
        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);


        notificationService.notifyShare(post.getUser(), currentUser, postId);

        log.info("User {} successfully shared post {}", currentUser.getUsername(), postId);
        return shareMapper.toResponse(savedShare);
    }
}
