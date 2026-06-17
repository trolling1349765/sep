package fpt.capstone.dto.response;

import fpt.capstone.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserResponse {
    String id;
    String role;
    String name;
    String email;
    String username;
    Date dob;
    Date createAt;
    String createBy;
    Date updateAt;
    String updateBy;
    boolean isDelete;
}
