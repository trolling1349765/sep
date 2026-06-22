package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "goods_inventories")
public class GoodsInventory extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    Sponsor sponsor;

    @Column(name = "item_name")
    String itemName;

    @Column(name = "quantity")
    int quantity;

    @OneToMany(mappedBy = "goodsInventory")
    List<GoodsDistribution> goodsDistributionList;
}
