package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Append-only manual stock adjustment (Dieu chinh ton). Records the signed delta, a reason,
 * and the resulting on-hand balance.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "stock_adjustments")
public class StockAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    SupportItem item;

    @Column(name = "delta_qty")
    int deltaQty;

    @Column(name = "reason")
    String reason;

    @Column(name = "balance_after")
    int balanceAfter;

    @Column(name = "adjusted_by")
    String adjustedBy;

    @Column(name = "adjusted_at")
    LocalDateTime adjustedAt;
}
