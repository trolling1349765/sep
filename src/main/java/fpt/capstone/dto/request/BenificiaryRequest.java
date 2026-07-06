package fpt.capstone.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BenificiaryRequest {
    Integer applicationId;
    boolean gender;
    Double assistanceAmount;
    String fullName;
    String codeName;
    LocalDate dob;
    String CCCD;
    LocalDate issuedDate;
    String issuedPlace;
    String hometown;
    String placeOfResidence;
    LocalDate joinPartyDate;
    LocalDate officialDate;
    String rank;
    String workUnit;
    LocalDate beginRevolutionaryActivities;
    LocalDate endRevolutionaryActivities;
    LocalDate sacrificeDate;
    String sacrificeAt;
    String rankWhenSacrifice;
    Integer nationMeritNumber;
    Integer decisionNumberOfMerit;
    LocalDate recognizedDate;
}
