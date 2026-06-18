package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    String id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "role_id")
    Role role;

    @Column(name = "name")
    String name;

    @Column(name = "email")
    String email;

    @Column(name = "username")
    String username;

    @Column(name = "password")
    String password;

    @Column(name = "dob")
    Date dob;

    @OneToMany(mappedBy = "supportedUser")
    private List<Application> supportedApplications;

    @OneToMany(mappedBy = "approvedBy")
    private List<Application> approvedApplications;

    @OneToMany(mappedBy = "issuer")
    List<DecisionDocument> decisionDocuments;

    @OneToMany(mappedBy = "user")
    List<Benificiary> benificiarys;

    @OneToMany(mappedBy = "deliver")
    List<BenefitHistory> benefitHistories;
}
