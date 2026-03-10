package org.revature.revconnect.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionStatsResponse {

    private Long userId;
    private long followersCount;
    private long followingCount;
    private boolean isFollowing;
    private boolean isFollowedBy;
}
