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
@Table(name = "benificiaries")
public class Benificiary extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "assistance_amount")
    Double assistanceAmount;

    @OneToMany(mappedBy = "benificiary")
    List<DistributionRecord> distributionRecords;

    @OneToMany(mappedBy = "benificiary")
    List<BenefitHistory> benefitHistories;

    @OneToMany(mappedBy = "benificiary")
    List<GoodsDistribution> goodsDistributionList;
}
