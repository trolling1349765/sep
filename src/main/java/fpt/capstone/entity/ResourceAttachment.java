package fpt.capstone.entity;

import fpt.capstone.enums.AttachmentKind;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Polymorphic file attachment shared by every donor/resource owner (sponsor, funding, plan,
 * receipt, distribution...). {@code ownerType} is the audit {@link fpt.capstone.enums.Table}
 * enum stored as STRING; {@code ownerId} is the owner id as text (int/UUID both toString()'d).
 * {@code fileUrl} holds the stored file name returned by {@code FileStorageService.storeFile}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "resource_attachments")
public class ResourceAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    // Fully-qualified to avoid clashing with jakarta.persistence.Table used on this class.
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    fpt.capstone.enums.Table ownerType;

    @Column(name = "owner_id")
    String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    AttachmentKind kind;

    @Column(name = "file_url", length = 500)
    String fileUrl;

    @Column(name = "file_name")
    String fileName;

    @Column(name = "uploaded_by")
    String uploadedBy;

    @Column(name = "uploaded_at")
    LocalDateTime uploadedAt;
}
