package fpt.capstone.dto.response;

import fpt.capstone.entity.Relative;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RelativeResponse {
    int id;
    String fullName;
    LocalDate dob;
    String CCCD;
    LocalDate issuedDate;
    String issuedPlace;
    String relationshipWithBenificiary;
    String placeOfResidence;
    String phone;
    String email;
    boolean gender;

    public RelativeResponse(Relative relative) {
        this.id = relative.getId();
        this.fullName = relative.getFullName();
        this.dob = relative.getDob();
        this.CCCD = relative.getCCCD();
        this.issuedDate = relative.getIssuedDate();
        this.issuedPlace = relative.getIssuedPlace();
        this.relationshipWithBenificiary = relative.getRelationshipWithBenificiary();
        this.placeOfResidence = relative.getPlaceOfResidence();
        this.phone = relative.getPhone();
        this.email = relative.getEmail();
        this.gender = relative.isGender();
    }
}
