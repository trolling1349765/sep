package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(name = "benefit_rule")
public class BenefitRule extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    Policy policy;

    @Column(name = "formula")
    String formula;

    @Column(name = "benchmark")
    String benchmark;

    @Column(name = "multiplier")
    Double multiplier = 1.0;


}
