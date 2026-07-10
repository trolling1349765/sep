package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "sponsors")
public class Sponsor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "name")
    String name;

    @Column(name = "sponsor_type")
    String sponsorType; // CA_NHAN, TO_CHUC

    @Column(name = "contact_info")
    String contactInfo;

    @Column(name = "phone")
    String phone;

    @Column(name = "email")
    String email;

    @Column(name = "address")
    String address;

    @Column(name = "representative")
    String representative;

    @Column(name = "tax_code")
    String taxCode;

    @Column(name = "status")
    String status; // ACTIVE, INACTIVE

    @JsonIgnore
    @OneToMany(mappedBy = "sponsor")
    List<Donation> donations;

    @JsonIgnore
    @OneToMany(mappedBy = "sponsor")
    List<GoodsInventory> goodsInventories;
}