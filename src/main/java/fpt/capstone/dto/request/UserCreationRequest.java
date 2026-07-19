package fpt.capstone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @NotNull(message = "ROLE_NOT_FOUND")
    Integer roleId;

    @NotBlank(message = "ARGUMENT_INVALID")
    @Size(min = 1, max = 100, message = "ARGUMENT_INVALID")
    String name;

    @NotBlank(message = "ARGUMENT_INVALID")
    @Email
    String email;

    // VN mobile format: 0xxxxxxxxx or +84xxxxxxxxx
    @NotBlank(message = "ARGUMENT_INVALID")
    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "ARGUMENT_INVALID")
    String phone;

    @NotBlank(message = "ARGUMENT_INVALID")
    @Size(min = 4, max = 50, message = "ARGUMENT_INVALID")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "ARGUMENT_INVALID")
    String username;

    @NotBlank(message = "PASSWORD_INVALID")
    @Size(min = 8, max = 20, message = "PASSWORD_INVALID")
    String password;

    // Chức danh — required on the admin create modal
    @NotBlank(message = "ARGUMENT_INVALID")
    @Size(min = 1, max = 128, message = "ARGUMENT_INVALID")
    String position;

    // Địa bàn phụ trách — blank falls back to app.default-assigned-area
    @Size(max = 255, message = "ARGUMENT_INVALID")
    String assignedArea;

    LocalDate dob;
}
