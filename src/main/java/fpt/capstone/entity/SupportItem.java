package fpt.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Catalog of donated support items (rice, blankets, wheelchairs, ...). One inventory row
 * ({@link GoodsInventory}) is kept per item.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "support_items", indexes = {
        @Index(name = "idx_support_item_normalized_name", columnList = "normalized_name")
})
public class SupportItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "code", unique = true)
    String code;

    @Column(name = "name")
    String name;

    // trim + lowercase + strip accents — used for duplicate detection (ITEM_DUPLICATED).
    @Column(name = "normalized_name")
    String normalizedName;

    @Column(name = "unit")
    String unit;

    @Column(name = "description")
    String description;
}
