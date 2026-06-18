package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(name = "applications")
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User supportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    User approvedBy;

    @Column(name = "approved_date")
    Date approvedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    Policy policy;

    @Column(name = "submit_date")
    Date submitDate;

    @Column(name = "status")
    String status;

    @Column(name = "form_type")
    String formType;

    @OneToMany(mappedBy = "application")
    List<ApplicationDetail> applicationDetails;

    @OneToOne(mappedBy = "application")
    DecisionDocument decisionDocument;

    @OneToMany(mappedBy = "application")
    List<AdditionalDocument> additionalDocuments;
}
