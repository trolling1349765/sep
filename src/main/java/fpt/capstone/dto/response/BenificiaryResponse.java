package fpt.capstone.dto.response;

import fpt.capstone.entity.Benificiary;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@NoArgsConstructor
public class BenificiaryResponse {
    int id;
    Double assistanceAmount;
    int relativeId;
    boolean gender;

    public BenificiaryResponse(Benificiary benificiary) {
        this.id = benificiary.getId();
        this.assistanceAmount = benificiary.getAssistanceAmount();
        this.relativeId = benificiary.getRelative().getId();
        this.gender = benificiary.isGender();
    }
}
