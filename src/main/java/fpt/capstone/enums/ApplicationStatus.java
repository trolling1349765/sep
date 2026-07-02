package fpt.capstone.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public enum ApplicationStatus {
    PENDING("PENDING"),
    CHECKED("CHECKED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    ;

    String status;

    public String get() {
        return status;
    }
}
