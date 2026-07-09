package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @NotBlank(message = "Email or phone number is required")
    String login;

    @NotBlank(message = "Password is required")
    String password;

    @NotBlank(message = "Captcha ID is required")
    String captchaId;

    @NotBlank(message = "Captcha code is required")
    String captchaCode;
}