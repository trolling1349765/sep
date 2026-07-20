package fpt.capstone.entity;

import fpt.capstone.enums.PlanLineStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * One beneficiary line of an {@link ItemAllocationPlan}. {@code issuedQty} accumulates delivered
 * quantity; the remaining reserved amount = plannedQty - issuedQty until cancel/not-delivered.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "item_allocation_plan_lines")
public class ItemAllocationPlanLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    ItemAllocationPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    Benificiary beneficiary;

    @Column(name = "planned_qty")
    int plannedQty;

    @Builder.Default
    @Column(name = "issued_qty")
    int issuedQty = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "line_status")
    PlanLineStatus lineStatus = PlanLineStatus.PENDING;

    @Column(name = "not_delivered_note")
    String notDeliveredNote;
}
