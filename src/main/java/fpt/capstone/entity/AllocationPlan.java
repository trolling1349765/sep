package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "allocation_plans")
public class AllocationPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "plan_code", unique = true)
    String planCode;

    @Column(name = "title")
    String title;

    @Column(name = "description")
    String description;

    @Column(name = "status")
    String status; // DRAFT, CONFIRMED, COMPLETED, CANCELLED

    @Column(name = "planned_date")
    LocalDate plannedDate;

    @Column(name = "completed_date")
    LocalDate completedDate;

    @Column(name = "notes")
    String notes;

    @JsonIgnore
    @OneToMany(mappedBy = "allocationPlan")
    List<GoodsDistribution> goodsDistributions;
}