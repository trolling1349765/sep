package fpt.capstone.entity;

import fpt.capstone.enums.SupportCategory;
import fpt.capstone.enums.SupportRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "support_requests")
public class SupportRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @Column(name = "reference_number", unique = true, nullable = false, length = 50)
    String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    User citizen;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    SupportCategory category;

    @Column(name = "subject", nullable = false, length = 255)
    String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    SupportRequestStatus status = SupportRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    User assignedTo;

    @Column(name = "resolved_at")
    Instant resolvedAt;

    @OneToMany(mappedBy = "supportRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    List<SupportRequestAttachment> attachments;

    @OneToMany(mappedBy = "supportRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    List<SupportReply> replies;
}