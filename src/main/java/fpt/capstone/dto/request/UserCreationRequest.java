package fpt.capstone.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class UserCreationRequest {
    private String userId;
    private String name;
    private String email;
    private String username;
    @Size(min = 8, max = 20, message = "password must be in range 8 and 20 digits ")
    private String password;
    private Date dob;
}
