package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "token", nullable = false, length = 512)
    String token;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "family_id", nullable = false)
    String familyId;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Builder.Default
    @Column(name = "revoked", nullable = false)
    boolean revoked = false;

    @Column(name = "revoked_at")
    Instant revokedAt;
}