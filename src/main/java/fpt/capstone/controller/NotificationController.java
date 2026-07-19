package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.NotificationResponse;
import fpt.capstone.entity.User;
import fpt.capstone.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
// Single catalogue right covers the whole notification surface (view/mark/delete)
@PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<APIResponse<Page<NotificationResponse>>> getUserNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                user.getId(), page, size);
        return ResponseEntity.ok(APIResponse.success(notifications));
    }

    @GetMapping("/unread")
    public ResponseEntity<APIResponse<List<NotificationResponse>>> getUnreadNotifications(
            @AuthenticationPrincipal User user) {
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(
                user.getId());
        return ResponseEntity.ok(APIResponse.success(notifications));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<APIResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(APIResponse.success(count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<APIResponse<Void>> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(APIResponse.success(null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<APIResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(APIResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.ok(APIResponse.success(null));
    }
}
