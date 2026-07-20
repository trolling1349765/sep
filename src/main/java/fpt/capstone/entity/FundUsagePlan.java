package fpt.capstone.entity;

import fpt.capstone.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Money usage plan (Ke hoach su dung kinh phi) drawn against a CONFIRMED {@link Donation}.
 * Creator = inherited {@code BaseEntity.createBy}. State machine: PlanStatus.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "fund_usage_plans")
public class FundUsagePlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "code", unique = true)
    String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    Donation donation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    Benificiary beneficiary;

    @Column(name = "program_name")
    String programName;

    @Column(name = "amount", precision = 15, scale = 0)
    BigDecimal amount;

    @Column(name = "purpose")
    String purpose;

    @Column(name = "expected_date")
    LocalDate expectedDate;

    @Column(name = "delivery_place")
    String deliveryPlace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    PlanStatus status;

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @Column(name = "approved_by")
    String approvedBy;

    @Column(name = "approved_at")
    LocalDateTime approvedAt;

    @Column(name = "reject_reason")
    String rejectReason;

    @Column(name = "cancel_reason")
    String cancelReason;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @Column(name = "delete_reason")
    String deleteReason;

    @Column(name = "deleted_by")
    String deletedBy;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;
}
