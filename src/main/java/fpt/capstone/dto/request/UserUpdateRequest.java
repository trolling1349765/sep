package fpt.capstone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Partial update: every field optional, non-null values are patched.
 * The target user id comes from the path — no userId field here.
 */
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(min = 1, max = 100, message = "ARGUMENT_INVALID")
    String name;

    @Email
    String email;

    // @Pattern passes on null, so the field stays optional
    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "ARGUMENT_INVALID")
    String phone;

    @Size(min = 8, max = 20, message = "PASSWORD_INVALID")
    String password;

    @Size(min = 1, max = 128, message = "ARGUMENT_INVALID")
    String position;

    @Size(max = 255, message = "ARGUMENT_INVALID")
    String assignedArea;

    LocalDate dob;

    Integer roleId;
}
