package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    String id;
    @OneToOne(cascade = CascadeType.ALL)
    Role role;
    String name;
    String email;
    String username;
    String password;
    Date dob;
}
