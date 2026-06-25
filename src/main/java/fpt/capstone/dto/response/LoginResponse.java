package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    String userId;
    String email;
    String name;
    String role;
    Instant accessTokenExpiresAt;
    String message;

    public static LoginResponse fromUser(fpt.capstone.entity.User user, Instant accessTokenExpiresAt) {
        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .build();
    }
}