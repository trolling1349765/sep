package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fpt.capstone.enums.ApplicationStatus;
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
@Table(name = "applications")
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    User approvedBy;

    @Column(name = "approve_date")
    LocalDate approveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(name = "submit_by")
    User submitBy;

    @Column(name = "submit_date")
    LocalDate submitDate;

    @Column(name = "status", columnDefinition = "TEXT")
    ApplicationStatus status;

    @JoinColumn(name = "form_type_id")
    @ManyToOne(fetch = FetchType.LAZY)
    FormType formType;

    @OneToOne(mappedBy = "application")
    DecisionDocument decisionDocument;

    @JsonIgnore
    @OneToMany(mappedBy = "application")
    List<AdditionalDocument> additionalDocuments;

    @OneToOne(mappedBy = "application")
    Benificiary benificiary;

    @OneToOne(mappedBy = "application")
    Relative relative;
}
