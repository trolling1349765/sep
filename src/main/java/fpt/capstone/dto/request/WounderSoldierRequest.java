package fpt.capstone.dto.request;

import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WounderSoldierRequest {
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

    public boolean isEmpty() {
        if (WounderSoldierRequest.this.isEmpty()) return true;
        return false;
    }
}
