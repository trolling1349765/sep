package fpt.capstone.entity;

import jakarta.persistence.*;

@Entity
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;
}
