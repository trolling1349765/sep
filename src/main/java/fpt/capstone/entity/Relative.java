package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "relatives")
public class Relative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "dob")
    LocalDate dob;

    @Column(name = "cccd")
    String CCCD;

    @Column(name = "issued_date")
    LocalDate issuedDate;

    @Column(name = "issued_place")
    String issuedPlace;

    @Column(name = "relationship_with_Benificiary")
    String relationshipWithBenificiary;

    @Column(name = "place_of_residence")
    String noiThuongTru;

    @Column(name = "phone")
    String phone;

    @Column(name = "email")
    String email;

    @Column(name = "gender")
    boolean gender;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "application_id")
    Application application;
}
