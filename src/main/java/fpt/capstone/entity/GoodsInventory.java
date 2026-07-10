package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

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
    @JoinColumn(name = "sponsor_id")
    Sponsor sponsor;

    @Column(name = "item_name")
    String itemName;

    @Column(name = "unit")
    String unit; // KG, CAI, THUNG, etc.

    @Column(name = "quantity")
    int quantity;

    @Column(name = "reserved_quantity")
    int reservedQuantity;

    @Column(name = "condition_status")
    String conditionStatus; // NEW, GOOD, DAMAGED

    @Column(name = "location")
    String location;

    @Column(name = "receipt_date")
    LocalDate receiptDate;

    @Column(name = "status")
    String status; // AVAILABLE, RESERVED, DEPLETED

    @Column(name = "notes")
    String notes;

    @JsonIgnore
    @OneToMany(mappedBy = "goodsInventory")
    List<GoodsDistribution> goodsDistributionList;
}