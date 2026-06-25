package fpt.capstone.dto.request;

import fpt.capstone.entity.Role;
import jakarta.validation.constraints.Email;
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

    Role role;
    String name;
    @Email
    String email;
    String username;
    @Size(min = 8, max = 20, message = "PASSWORD_INVALID")
    String password;
    LocalDate dob;
}
