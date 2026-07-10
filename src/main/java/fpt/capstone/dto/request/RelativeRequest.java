package fpt.capstone.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RelativeRequest {
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

    public boolean isEmpty() {
        if (RelativeRequest.this.isEmpty())
            return true;
        return false;
    }
}
