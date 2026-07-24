package fpt.capstone.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public enum ApplicationStatus {
    DRAFT("DRAFT"),             //Citizen save draft
    SUBMITTED("SUBMITTED"),
    PENDING("PENDING"),         //OFF1 tiep nhan xong
    CHECKED("WAITING_FOR_APPROVE"),         //OFF2 tham dinh XONG
    COMPLETED("APPROVED"),     //OFF3 + OFF4 duyet hs
    INSUFFICIENT("INSUFFICIENT"),
    REJECTED("REJECTED"),
    ;

    String status;

    public String get() {
        return status;
    }
}
