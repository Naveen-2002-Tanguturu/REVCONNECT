package org.revature.revconnect.dto.response;
import org.revature.revconnect.model.Share;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String profilePicture;
    private Long postId;
    private String comment;
    private LocalDateTime createdAt;

    public static ShareResponse fromEntity(Share share) {
        return ShareResponse.builder()
                .id(share.getId())
                .userId(share.getUser().getId())
                .username(share.getUser().getUsername())
                .name(share.getUser().getName())
                .profilePicture(share.getUser().getProfilePicture())
                .postId(share.getPost().getId())
                .comment(share.getComment())
                .createdAt(share.getCreatedAt())
                .build();
    }
}