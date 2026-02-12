package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.model.Connection;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionResponse {

    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String profilePicture;
    private String bio;
    private ConnectionStatus status;
    private LocalDateTime createdAt;

    public static ConnectionResponse fromFollower(Connection connection) {
        return ConnectionResponse.builder()
                .id(connection.getId())
                .userId(connection.getFollower().getId())
                .username(connection.getFollower().getUsername())
                .name(connection.getFollower().getName())
                .profilePicture(connection.getFollower().getProfilePicture())
                .bio(connection.getFollower().getBio())
                .status(connection.getStatus())
                .createdAt(connection.getCreatedAt())
                .build();
    }

    public static ConnectionResponse fromFollowing(Connection connection) {
        return ConnectionResponse.builder()
                .id(connection.getId())
                .userId(connection.getFollowing().getId())
                .username(connection.getFollowing().getUsername())
                .name(connection.getFollowing().getName())
                .profilePicture(connection.getFollowing().getProfilePicture())
                .bio(connection.getFollowing().getBio())
                .status(connection.getStatus())
                .createdAt(connection.getCreatedAt())
                .build();
    }
}
