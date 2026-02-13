package org.revature.revconnect.dto.response;

import org.revature.revconnect.model.Like;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String profilePicture;
    private LocalDateTime createdAt;

    public static LikeResponse fromEntity(Like like) {
        return LikeResponse.builder()
                .id(like.getId())
                .userId(like.getUser().getId())
                .username(like.getUser().getUsername())
                .name(like.getUser().getName())
                .profilePicture(like.getUser().getProfilePicture())
                .createdAt(like.getCreatedAt())
                .build();
    }
}