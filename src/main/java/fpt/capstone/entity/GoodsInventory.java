package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Stock balance — exactly one row per {@link SupportItem}.
 * Balance model: available = quantityOnHand - reservedQuantity (computed at runtime).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "goods_inventories")
public class GoodsInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", unique = true)
    SupportItem item;

    @Builder.Default
    @Column(name = "quantity_on_hand")
    int quantityOnHand = 0;

    @Builder.Default
    @Column(name = "reserved_quantity")
    int reservedQuantity = 0;

    /** @deprecated inventory rows are now keyed by {@link #item}, not a free-text name. */
    @Deprecated
    @Column(name = "item_name")
    String itemName;

    /** @deprecated inventory is no longer per-sponsor. */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    Sponsor sponsor;

    /** @deprecated replaced by {@link #quantityOnHand}/{@link #reservedQuantity}. */
    @Deprecated
    @Column(name = "quantity")
    int quantity;

    @JsonIgnore
    @OneToMany(mappedBy = "goodsInventory")
    List<GoodsDistribution> goodsDistributionList;
}
