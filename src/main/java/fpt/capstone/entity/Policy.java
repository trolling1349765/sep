package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
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
    Date issuedDate;

    @Column(name = "effective_date")
    Date effectiveDate;

    @Column(name = "expired_date")
    Date expiredDate;

    @Column(name = "issuer")
    String issuer;

    @Column(name = "summary")
    String summary;

    @Column(name = "file_url")
    String fileURL;

    @OneToMany(mappedBy = "policy")
    List<Article> articles;

    @OneToMany(mappedBy = "policy")
    List<EligibilityCriteria> eligibilityCriterias;

    @OneToMany(mappedBy = "policy")
    List<Application> applications;

    @OneToOne(mappedBy = "policy")
    BenefitRule benefitRule;
}
