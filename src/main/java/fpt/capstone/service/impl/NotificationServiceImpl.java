package fpt.capstone.service.impl;

import fpt.capstone.dto.response.NotificationResponse;
import fpt.capstone.entity.Notification;
import fpt.capstone.entity.User;
import fpt.capstone.enums.NotificationType;
import fpt.capstone.repository.NotificationRepository;
import fpt.capstone.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendNotification(User user, NotificationType type, String title, String message,
            String referenceType, String referenceId, String imageUrl, String actionUrl) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .imageUrl(imageUrl)
                .actionUrl(actionUrl)
                .createdAt(Instant.now())
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        log.info("Notification sent to user {}: {}", user.getEmail(), title);
    }

    @Override
    @Transactional
    public void sendNotificationToAll(NotificationType type, String title, String message,
            String referenceType, String referenceId, String imageUrl, String actionUrl) {
        log.warn("sendNotificationToAll not implemented without UserRepository reference. Implement via bulk lookup.");
    }

    @Override
    @Transactional
    public void sendBulkNotification(List<User> users, NotificationType type, String title, String message,
            String referenceType, String referenceId, String imageUrl, String actionUrl) {
        Instant now = Instant.now();
        List<Notification> notifications = users.stream()
                .map(user -> Notification.builder()
                        .user(user)
                        .type(type)
                        .title(title)
                        .message(message)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .imageUrl(imageUrl)
                        .actionUrl(actionUrl)
                        .createdAt(now)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
        log.info("Bulk notification sent to {} users", users.size());
    }

    @Override
    public Page<NotificationResponse> getUserNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId, String userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, Instant.now());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found.");
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found."));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        notificationRepository.delete(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .imageUrl(notification.getImageUrl())
                .actionUrl(notification.getActionUrl())
                .build();
    }
}