package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitizenCardResponse {
    private String citizenId;
    private String fullName;
    private String dateOfBirth;
    private String gender;
    private String address;
}
