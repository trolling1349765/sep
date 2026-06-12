package fpt.capstone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    String userId;
    String name;
    @Email
    String email;
    String username;
    @Size(min = 8, max = 20, message = "password must be in range 8 and 20 digits ")
    String password;
    Date dob;
}
