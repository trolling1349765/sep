package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
public class Policy extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    int id;

    @Column(name = "document_no")
    String documentNo;

    @Column(name = "title")
    String title;

    @Column(name = "document_type")
    String documentType;

    @Column(name = "issued_date")
    LocalDate issuedDate;

    @Column(name = "effective_date")
    LocalDate effectiveDate;

    @Column(name = "expired_date")
    LocalDate expiredDate;

    @Column(name = "issuer")
    String issuer;

    @Column(name = "summary")
    String summary;

    @Column(name = "file_url")
    String fileURL;

    @JsonIgnore
    @OneToMany(mappedBy = "policy")
    List<Chapter> chapters;

    @JsonIgnore
    @OneToMany(mappedBy = "policy")
    List<EligibilityCriteria> eligibilityCriterias;

    @JsonIgnore
    @OneToMany(mappedBy = "policy")
    List<FormType> formTypes;

    @OneToOne(mappedBy = "policy")
    BenefitRule benefitRule;
}
