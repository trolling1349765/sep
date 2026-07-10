package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "distribution_record")
public class DistributionRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benificiary_id")
    Benificiary benificiary;

    @Column(name = "quantity")
    int quantity;

    @Column(name = "transfer_date")
    LocalDate transferDate;

    @Column(name = "items_description")
    String itemsDescription;

    @Column(name = "notes")
    String notes;
}