package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "eligibility_criterias")
public class EligibilityCriteria extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    Policy policy;

    @Column(name = "applicable_subject")
    String applicableSubject;

    @Column(name = "condition_value")
    String conditionValue;

    @Column(name = "benchmark")
    String benchmark;
}
