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
public class RelativeRequest {
    Integer id;
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
    boolean isDeleted;

    public boolean isEmpty() {
        if (RelativeRequest.this.isEmpty()) return true;
        return false;
    }
}
