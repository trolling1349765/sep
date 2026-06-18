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
@Table(name = "benefit_history")
public class BenefitHistory extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benificiary_id")
    Benificiary benificiary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deliver")
    User deliver;

    @Column(name = "transfer_method")
    String transferMethod;
}
