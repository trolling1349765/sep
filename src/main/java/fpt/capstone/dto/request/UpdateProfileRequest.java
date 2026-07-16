package fpt.capstone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    String name;

    String nationalId;

    LocalDate dob;

    @Email(message = "Email must be valid.")
    String email;

    @Pattern(regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$", message = "Phone number must be 10 digits starting with 0 or +84.")
    String phone;

    String provinceCode;
    String provinceName;
    String wardCode;
    String wardName;
    String specificAddress;

    Boolean gender;
}