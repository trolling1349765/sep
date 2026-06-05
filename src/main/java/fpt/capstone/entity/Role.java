package fpt.capstone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Role extends BaseEntity {

    @Id
    private int id;
    private String name;
    private String description;
}
