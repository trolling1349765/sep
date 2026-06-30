package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "rights")
public class Right extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "right_id")
    int id;

    @Column(name = "code")
    String code;

    @Column(name = "name")
    String name;

    @Column(name = "description")
    String description;

    @JsonIgnore
    @OneToMany(mappedBy = "right")
    List<Permission> permissions;
}
