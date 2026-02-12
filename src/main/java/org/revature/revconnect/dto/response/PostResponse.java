package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.model.Post;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private Long id;
    private String content;
    private PostType postType;
    private List<String> mediaUrls;
    private Boolean pinned;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Author info
    private Long authorId;
    private String authorUsername;
    private String authorName;
    private String authorProfilePicture;

    public static PostResponse fromEntity(Post post) {
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
