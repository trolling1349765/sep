package fpt.capstone.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class UserCreationRequest {
    private String name;
    private String email;
    private String username;
    private String password;
    private Date dob;
}
