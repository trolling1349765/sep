package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "goods_distributions")
public class GoodsDistribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_plan_id")
    AllocationPlan allocationPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_inventory_id")
    GoodsInventory goodsInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benificiary_id")
    Benificiary benificiary;

    @Column(name = "quantity")
    int quantity;

    @Column(name = "transfer_date")
    LocalDate transferDate;

    @Column(name = "confirmation_status")
    String confirmationStatus; // PENDING, CONFIRMED, ABSENT

    @Column(name = "notes")
    String notes;
}