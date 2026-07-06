package fpt.capstone.dto.response;

import fpt.capstone.entity.WoundedSoldiers;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WounderSoldierResponse {
    Integer id;
    Integer benificiaryId;
    Integer times;
    LocalDate enlistmentDate;
    LocalDate dischargeDate;
    String takeDmgAt;
    LocalDate takeDmgDate;
    String rankWhenTakeDmg;
    String injuredArea;
    String wound;
    String treatmentPlace;
    LocalDate injuryHealedDate;

    public WounderSoldierResponse(WoundedSoldiers woundedSoldiers) {
        this.id = woundedSoldiers.getId();
        this.benificiaryId = woundedSoldiers.getBenificiary().getId();
        this.times = woundedSoldiers.getTimes();
        this.enlistmentDate = woundedSoldiers.getEnlistmentDate();
        this.dischargeDate = woundedSoldiers.getDischargeDate();
        this.takeDmgAt = woundedSoldiers.getTakeDmgAt();
        this.takeDmgDate = woundedSoldiers.getTakeDmgDate();
        this.rankWhenTakeDmg = woundedSoldiers.getRankWhenTakeDmg();
        this.injuredArea = woundedSoldiers.getInjuredArea();
        this.wound = woundedSoldiers.getWound();
        this.treatmentPlace = woundedSoldiers.getTreatmentPlace();
        this.injuryHealedDate = woundedSoldiers.getInjuryHealedDate();
    }
}
