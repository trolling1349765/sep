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
@Table(name = "benificiaries")
public class Benificiary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @JsonIgnore
    @OneToMany(mappedBy = "benificiary")
    List<DistributionRecord> distributionRecords;

    @JsonIgnore
    @OneToMany(mappedBy = "benificiary")
    List<BenefitHistory> benefitHistories;

    @JsonIgnore
    @OneToMany(mappedBy = "benificiary")
    List<GoodsDistribution> goodsDistributionList;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "application_id")
    Application application;

    @Column(name = "gender")
    boolean gender;

    @Column(name = "assistance_amount")
    Double assistanceAmount;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "code_name")
    String codeName;

    @Column(name = "dob")
    LocalDate dob;

    @Column(name = "cccd")
    String CCCD;

    @Column(name = "issued_date")
    LocalDate issuedDate;

    @Column(name = "issued_place")
    String issuedPlace;

    @Column(name = "hometown")
    String hometown;

    @Column(name = "place_of_residence")
    String placeOfResidence;

    @Column(name = "join_party_date")
    LocalDate joinPartyDate;

    @Column(name = "official_date")
    LocalDate officialDate;

    @Column(name = "benificiary_rank")
    String benificiaryRank;

    @Column(name = "work_unit")
    String workUnit;

    @Column(name = "begin_revolutionary_activities")
    LocalDate beginRevolutionaryActivities;

    @Column(name = "end_revolutionary_activities")
    LocalDate endRevolutionaryActivities;

    @OneToMany(mappedBy = "benificiary")
    List<WoundedSoldiers> woundedSoldiers;

    @Column(name = "sacrifice_date")
    LocalDate sacrificeDate;

    @Column(name = "sacrifice_at")
    String sacrificeAt;

    @Column(name = "rank_when_sacrifice")
    String rankWhenSacrifice;

    @Column(name = "nation_merit_number")
    Integer nationMeritNumber;

    @Column(name = "decision_number_of_merit")
    Integer decisionNumberOfMerit;

    @Column(name = "recognized_date")
    LocalDate recognizedDate;

}