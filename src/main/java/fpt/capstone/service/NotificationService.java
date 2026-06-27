package fpt.capstone.service;

import fpt.capstone.dto.response.NotificationResponse;
import fpt.capstone.entity.User;
import fpt.capstone.enums.NotificationType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    void sendNotification(User user, NotificationType type, String title, String message,
            String referenceType, String referenceId, String imageUrl, String actionUrl);

    void sendNotificationToAll(NotificationType type, String title, String message,
            String referenceType, String referenceId, String imageUrl, String actionUrl);

    void sendBulkNotification(List<User> users, NotificationType type, String title, String message,
            String referenceType, String referenceId, String imageUrl, String actionUrl);

    Page<NotificationResponse> getUserNotifications(String userId, int page, int size);

    List<NotificationResponse> getUnreadNotifications(String userId);

    long getUnreadCount(String userId);

    void markAsRead(String notificationId, String userId);

    void markAllAsRead(String userId);

    void deleteNotification(String notificationId, String userId);
}