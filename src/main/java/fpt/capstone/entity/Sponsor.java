package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(name = "sponsors")
public class Sponsor extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "name")
    String name;

    @Column(name = "contact_info")
    String contactInfo;

    @OneToMany(mappedBy = "sponsor")
    List<Donation> donations;

    @OneToMany(mappedBy = "sponsor")
    List<GoodsInventory> goodsInventories;
}
