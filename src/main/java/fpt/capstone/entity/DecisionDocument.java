package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "decision_document")
public class DecisionDocument extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "decision_number")
    String decisionNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer")
    User issuer;

    @Column(name = "issue_date")
    LocalDate issueDate;

    @Column(name = "details")
    String details;

    @Column(name = "file_path")
    String filePath;
}
