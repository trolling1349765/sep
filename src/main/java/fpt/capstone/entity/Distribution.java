package fpt.capstone.entity;

import fpt.capstone.enums.DistributionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single distribution record (Cap phat) against one plan line. Append-only history.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "distributions")
public class Distribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "code", unique = true)
    String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_line_id")
    ItemAllocationPlanLine planLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    Benificiary beneficiary;

    @Column(name = "recipient_name")
    String recipientName;

    @Column(name = "recipient_relationship")
    String recipientRelationship;

    @Column(name = "actual_qty")
    int actualQty;

    @Column(name = "issue_date")
    LocalDate issueDate;

    @Column(name = "issuing_officer")
    String issuingOfficer;

    @Column(name = "note")
    String note;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    DistributionStatus status = DistributionStatus.ISSUED;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;
}
