package fpt.capstone.entity;

import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Funding source (Nguon kinh phi) — money received from a sponsor/donation.
 * Balance model: available = amount - pendingAmount - spentAmount (computed at runtime).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "donations")
public class Donation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "code", unique = true)
    String code;

    @Column(name = "name")
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    Sponsor sponsor;

    @Column(name = "amount", precision = 15, scale = 0)
    BigDecimal amount;

    @Builder.Default
    @Column(name = "pending_amount", precision = 15, scale = 0)
    BigDecimal pendingAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "spent_amount", precision = 15, scale = 0)
    BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "purpose")
    String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    PaymentMethod paymentMethod;

    @Column(name = "transaction_ref")
    String transactionRef;

    @Column(name = "evidence_name")
    String evidenceName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    FundingStatus status = FundingStatus.DRAFT;

    @Column(name = "recorded_by")
    String recordedBy;

    // Used as the "received date" (ngay tiep nhan) of the funding.
    @Column(name = "transfer_date")
    LocalDate transferDate;
}
