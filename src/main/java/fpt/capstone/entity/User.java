package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.openid.connect.sdk.claims.Gender;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import fpt.capstone.enums.AccountStatus;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "national_id")
})
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
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

    @Column(name = "national_id", length = 12)
    String nationalId;

    @Column(name = "username")
    String username;

    @Column(name = "password")
    String password;

    @Column(name = "phone")
    String phone;

    @Column(name = "address")
    String address;

    @Column(name = "province_code")
    String provinceCode;

    @Column(name = "province_name")
    String provinceName;

    @Column(name = "district_code")
    String districtCode;

    @Column(name = "district_name")
    String districtName;

    @Column(name = "ward_code")
    String wardCode;

    @Column(name = "ward_name")
    String wardName;

    @Column(name = "specific_address")
    String specificAddress;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    AccountStatus status;

    @Column(name = "dob")
    LocalDate dob;

    @Column(name = "national_id_verified")
    Boolean nationalIdVerified;

    @JsonIgnore
    @OneToMany(mappedBy = "approvedBy")
    List<Application> approvedApplications;

    @JsonIgnore
    @OneToMany(mappedBy = "issuer")
    List<DecisionDocument> decisionDocuments;

    @JsonIgnore
    @OneToMany(mappedBy = "deliver")
    List<BenefitHistory> benefitHistories;

    @Builder.Default
    @Column(name = "failed_login_attempts")
    Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    Instant lockedUntil;

    @Column(name = "last_login_at")
    Instant lastLoginAt;

    @Column(name = "gender")
    boolean gender;

    @Column(name = "password_reset_token", length = 512)
    String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    Instant passwordResetTokenExpiry;

    @Builder.Default
    @Column(name = "password_reset_used")
    boolean passwordResetUsed = false;
}
