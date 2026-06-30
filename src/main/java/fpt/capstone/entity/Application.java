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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
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
    LocalDate approvedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    Policy policy;

    @Column(name = "submit_date")
    LocalDate submitDate;

    @Column(name = "status")
    String status;

    @Column(name = "form_type")
    String formType;

    @Column(name = "address")
    String address;

    @Column(name = "support_reason")
    String supportReason;

    @Column(name = "requested_amount")
    Double requestedAmount;

    @OneToOne(mappedBy = "application")
    DecisionDocument decisionDocument;

    @JsonIgnore
    @OneToMany(mappedBy = "application")
    List<AdditionalDocument> additionalDocuments;
}
