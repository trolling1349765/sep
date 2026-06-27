package fpt.capstone.dto.response;

import fpt.capstone.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;
    private boolean isRead;
    private Instant createdAt;
    private Instant readAt;
    private String imageUrl;
    private String actionUrl;
}