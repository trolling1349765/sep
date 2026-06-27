package fpt.capstone.entity;

import fpt.capstone.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    String message;

    @Column(name = "reference_type", length = 100)
    String referenceType;

    @Column(name = "reference_id")
    String referenceId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "read_at")
    Instant readAt;

    @Column(name = "image_url")
    String imageUrl;

    @Column(name = "action_url")
    String actionUrl;
}