package fpt.capstone.entity;

import fpt.capstone.enums.ItemCondition;
import fpt.capstone.enums.ReceiptStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inbound goods receipt (Phieu nhap kho). DRAFT (tiep nhan) -> POSTED (ghi so, cong ton).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "inbound_receipts")
public class InboundReceipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "code", unique = true)
    String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    SupportItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    Sponsor sponsor;

    @Column(name = "deliverer_name")
    String delivererName;

    @Column(name = "quantity")
    int quantity;

    // "condition" is a reserved word in MySQL — map to item_condition.
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition")
    ItemCondition condition;

    @Column(name = "receive_date")
    LocalDate receiveDate;

    @Column(name = "evidence_name")
    String evidenceName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    ReceiptStatus status = ReceiptStatus.DRAFT;

    @Column(name = "posted_by")
    String postedBy;

    @Column(name = "posted_at")
    LocalDateTime postedAt;
}
