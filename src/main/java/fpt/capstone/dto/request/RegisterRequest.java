package fpt.capstone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = "Full name is required.")
    String fullName;

    @NotNull(message = "Date of birth is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate dateOfBirth;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be a valid format.")
    String email;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$", message = "Phone must be 10 digits starting with 0 or +84.")
    String phone;

    @NotBlank(message = "Password is required.")
    String password;

    @NotBlank(message = "Password confirmation is required.")
    String passwordConfirmation;
}