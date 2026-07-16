package fpt.capstone.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public enum ApplicationStatus {
    DRAFT("Draft"),
    SUBMITTED("SUBMITTED"),
    CHECKED("CHECKED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    INSUFFICIENT("INSUFFICIENT"),
    ;

    String status;

    public String get() {
        return status;
    }
}
