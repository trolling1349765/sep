package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "system_log")
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "user_id")
    String userId;

    @Column(name = "action")
    String action;

    @Column(name = "entity_type")
    String entityType;

    @Column(name = "entity_id")
    String entityId;

    @Column(name = "old_value",columnDefinition = "TEXT")
    Object oldValue;

    @Column(name = "new_value",columnDefinition = "TEXT")
    Object newValue;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
