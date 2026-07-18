package fpt.capstone.dto.response;

import fpt.capstone.entity.Benificiary;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;


@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@NoArgsConstructor
public class BenificiaryResponse {
    Integer id;
    Integer applicationId;
    Boolean gender;
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

    public BenificiaryResponse(Benificiary benificiary) {
        this.id = benificiary.getId();
        this.applicationId = benificiary.getApplication().getId();
        this.gender = benificiary.isGender();
        this.assistanceAmount = benificiary.getAssistanceAmount();
        this.fullName = benificiary.getFullName();
        this.codeName = benificiary.getCodeName();
        this.dob = benificiary.getDob();
        this.CCCD = benificiary.getCCCD();
        this.issuedDate = benificiary.getIssuedDate();
        this.issuedPlace = benificiary.getIssuedPlace();
        this.hometown = benificiary.getHometown();
        this.placeOfResidence = benificiary.getPlaceOfResidence();
        this.joinPartyDate = benificiary.getJoinPartyDate();
        this.officialDate = benificiary.getOfficialDate();
        this.rank = benificiary.getRank();
        this.workUnit = benificiary.getWorkUnit();
        this.beginRevolutionaryActivities = benificiary.getBeginRevolutionaryActivities();
        this.endRevolutionaryActivities = benificiary.getEndRevolutionaryActivities();
        this.sacrificeDate = benificiary.getSacrificeDate();
        this.sacrificeAt = benificiary.getSacrificeAt();
        this.rankWhenSacrifice = benificiary.getRankWhenSacrifice();
        this.nationMeritNumber = benificiary.getNationMeritNumber();
        this.decisionNumberOfMerit = benificiary.getDecisionNumberOfMerit();
        this.recognizedDate = benificiary.getRecognizedDate();
    }
}
