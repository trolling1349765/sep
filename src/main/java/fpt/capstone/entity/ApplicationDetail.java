package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Table(name = "application_details")
public class ApplicationDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    Application application;

    @Column(name = "name")
    String name;

    @Column(name = "address")
    String address;

    @Column(name = "support_reason")
    String supportReason;

    @Column(name = "requested_amount")
    Double requestedAmount;

    @Column(name = "dob")
    Date dob;

    @Column(name = "identity_number")
    String identityNumber;

}
