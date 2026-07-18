package fpt.capstone.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public enum ApplicationStatus {
    DRAFT("Draft"),             //Citizen save draft
    SUBMITTED("Submitted"),
    PENDING("PENDING"),         //OFF1 tiep nhan
    CHECKED("CHECKED"),         //OFF2 tham dinh du
    IN_PROGRESS("IN_PROGRESS"), //OFF3 ra soat song
    COMPLETED("COMPLETED"),     //OFF4 duyet hs
    INSUFFICIENT("INSUFFICIENT"),
    ;

    String status;

    public String get() {
        return status;
    }
}
