package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
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
@Table(name = "sponsors", indexes = {
        @Index(name = "idx_sponsor_name", columnList = "name"),
        @Index(name = "idx_sponsor_org_code", columnList = "org_code"),
        @Index(name = "idx_sponsor_phone", columnList = "phone"),
        @Index(name = "idx_sponsor_normalized_name", columnList = "normalized_name")
})
public class Sponsor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "code", unique = true)
    String code;

    @Column(name = "name")
    String name;

    // trim + lowercase + strip accents — used for duplicate detection (BR-47 / SPONSOR_DUPLICATED).
    @Column(name = "normalized_name")
    String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    SponsorType type;

    @Column(name = "org_code", length = 50)
    String orgCode;

    @Column(name = "contact_person")
    String contactPerson;

    @Column(name = "phone")
    String phone;

    @Column(name = "email")
    String email;

    @Column(name = "address")
    String address;

    @Column(name = "note")
    String note;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    SponsorStatus status = SponsorStatus.ACTIVE;

    /** @deprecated replaced by discrete contactPerson/phone/email/address columns. */
    @Deprecated
    @Column(name = "contact_info")
    String contactInfo;

    @JsonIgnore
    @OneToMany(mappedBy = "sponsor")
    List<Donation> donations;

    @JsonIgnore
    @OneToMany(mappedBy = "sponsor")
    List<GoodsInventory> goodsInventories;
}
