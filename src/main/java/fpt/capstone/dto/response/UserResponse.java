package fpt.capstone.dto.response;

import fpt.capstone.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String role;
    String name;
    String email;
    String username;
    LocalDate dob;
    LocalDate createAt;
    String createBy;
    LocalDate updateAt;
    String updateBy;
    boolean isDelete;

    public UserResponse(User user) {
        this.id = user.getId();
        this.role = user.getRole().getName();
        this.name = user.getName();
        this.email = user.getEmail();
        this.username = getUsername();
        this.dob = user.getDob();
        this.createAt = user.getCreateAt();
        this.createBy = user.getCreateBy();
        this.updateAt = user.getUpdateAt();
        this.updateBy = user.getUpdateBy();
        this.isDelete = user.isDelete();
    }

}
