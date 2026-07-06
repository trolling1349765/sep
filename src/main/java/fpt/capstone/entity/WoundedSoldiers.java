package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "wounded_soldiers")
public class WoundedSoldiers extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benificiary_id")
    Benificiary benificiary;

    @Column(name = "times")
    Integer times;

    @Column(name = "enlistment_date")
    LocalDate enlistmentDate;

    @Column(name = "discharge_date")
    LocalDate dischargeDate;

    @Column(name = "take_dmg_at")
    String takeDmgAt;

    @Column(name = "take_dmg_date")
    LocalDate takeDmgDate;

    @Column(name = "rank_when_take_dmg")
    String rankWhenTakeDmg;

    @Column(name = "injured_area")
    String injuredArea;

    @Column(name = "wound")
    String wound;

    @Column(name = "treatment_place")
    String treatmentPlace;

    @Column(name = "injury_healed_date")
    LocalDate injuryHealedDate;
}
