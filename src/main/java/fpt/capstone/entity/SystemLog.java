package fpt.capstone.entity;

import com.ethlo.time.DateTime;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
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

    @Column(name = "old_value")
    Object oldValue;

    @Column(name = "new_value")
    Object newValue;

    @Column(name = "created_at")
    Date createdAt;
}
