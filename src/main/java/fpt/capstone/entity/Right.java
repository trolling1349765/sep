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

    @Column(name = "code", nullable = false, unique = true, length = 64)
    String code;

    @Column(name = "name")
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "module", nullable = false, length = 64)
    String module;

    @Column(name = "module_name", length = 128)
    String moduleName;

    @Builder.Default
    @Column(name = "is_system", nullable = false)
    boolean isSystem = false;

    @Column(name = "sort_order")
    Integer sortOrder;

    @JsonIgnore
    @OneToMany(mappedBy = "right")
    List<Permission> permissions;
}
