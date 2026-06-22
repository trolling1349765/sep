package fpt.capstone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    String userId;
    String name;
    @Email
    String email;
    @Size(min = 8, max = 20, message = "PASSWORD_INVALID")
    String password;
    Date dob;

}
