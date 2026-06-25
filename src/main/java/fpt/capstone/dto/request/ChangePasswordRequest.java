package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required.")
    String currentPassword;

    @NotBlank(message = "New password is required.")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters.")
    String newPassword;

    @NotBlank(message = "Confirm new password is required.")
    String confirmNewPassword;
}