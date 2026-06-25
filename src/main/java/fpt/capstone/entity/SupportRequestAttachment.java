package fpt.capstone.entity;

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
@Table(name = "support_request_attachments")
public class SupportRequestAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_request_id", nullable = false)
    SupportRequest supportRequest;

    @Column(name = "file_name", nullable = false, length = 255)
    String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    String fileType;

    @Column(name = "file_size", nullable = false)
    Long fileSize;

    @Column(name = "file_url", nullable = false, length = 500)
    String fileUrl;

    @Column(name = "uploaded_at", nullable = false)
    Instant uploadedAt;
}