package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "National ID must be exactly 12 digits.")
    String nationalId;

    @NotBlank(message = "Password is required")
    String password;
}