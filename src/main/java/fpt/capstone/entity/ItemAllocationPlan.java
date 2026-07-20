package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fpt.capstone.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Item allocation plan header (Ke hoach phan bo vat pham) — header + n lines. Reserves stock on
 * approve; auto-COMPLETEs when no PENDING line remains. Creator = inherited {@code BaseEntity.createBy}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "item_allocation_plans")
public class ItemAllocationPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "code", unique = true)
    String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    SupportItem item;

    @Column(name = "planned_qty")
    int plannedQty;

    @Column(name = "expected_date")
    LocalDate expectedDate;

    @Column(name = "delivery_place")
    String deliveryPlace;

    @Column(name = "delivery_time_window")
    String deliveryTimeWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    PlanStatus status;

    @Column(name = "approved_by")
    String approvedBy;

    @Column(name = "approved_at")
    LocalDateTime approvedAt;

    @Column(name = "reject_reason")
    String rejectReason;

    @Column(name = "cancel_reason")
    String cancelReason;

    @Column(name = "delete_reason")
    String deleteReason;

    @Column(name = "deleted_by")
    String deletedBy;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "plan")
    List<ItemAllocationPlanLine> lines;
}
