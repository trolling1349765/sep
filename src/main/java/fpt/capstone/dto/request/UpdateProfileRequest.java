package fpt.capstone.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    @Pattern(regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$", message = "Phone number must be 10 digits starting with 0 or +84.")
    String phone;

    String provinceCode;
    String provinceName;
    String wardCode;
    String wardName;
    String specificAddress;
}