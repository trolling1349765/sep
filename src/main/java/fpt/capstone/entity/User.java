package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private Role role;
    private String name;
    private String email;
    private String username;
    private String password;
}
