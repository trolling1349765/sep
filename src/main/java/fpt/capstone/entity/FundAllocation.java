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
@Table(name = "fund_allocations")
public class FundAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    Donation donation;

    @Column(name = "purpose")
    String purpose;

    @Column(name = "amount")
    Double amount;

    @Column(name = "beneficiary_type")
    String beneficiaryType;

    @Column(name = "beneficiary_id")
    String beneficiaryId;

    @Column(name = "beneficiary_name")
    String beneficiaryName;

    @Column(name = "allocation_date")
    LocalDate allocationDate;

    @Column(name = "notes")
    String notes;
}