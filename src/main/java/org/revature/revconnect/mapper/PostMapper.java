package org.revature.revconnect.mapper;

import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.model.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .postType(post.getPostType())
                .mediaUrls(post.getMediaUrls())
                .pinned(post.getPinned())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .authorId(post.getUser().getId())
                .authorUsername(post.getUser().getUsername())
                .authorName(post.getUser().getName())
                .authorProfilePicture(post.getUser().getProfilePicture())
                .build();
    }
}
