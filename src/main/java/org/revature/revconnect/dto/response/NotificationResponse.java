package org.revature.revconnect.dto.response;

import org.revature.revconnect.enums.NotificationType;
import org.revature.revconnect.model.Notification;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String message;
    private Long actorId;
    private String actorUsername;
    private String actorName;
    private String actorProfilePicture;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .actorId(notification.getActor().getId())
                .actorUsername(notification.getActor().getUsername())
                .actorName(notification.getActor().getName())
                .actorProfilePicture(notification.getActor().getProfilePicture())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
