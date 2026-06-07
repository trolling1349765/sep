package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    private Long id;
    @OneToOne(cascade = CascadeType.ALL)
    private Role role;
    private String name;
    private String email;
    private String username;
    private String password;
    private Date dob;

    public User (String name, String email, String username, String password, Date dob) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
        this.dob = dob;
    }
}
