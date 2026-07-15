package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import fpt.capstone.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    String userId;
    String name;
    String email;
    String nationalId;
    String phone;
    String address;
    String provinceCode;
    String provinceName;
    String wardCode;
    String wardName;
    String specificAddress;
    String fullAddress;
    String avatarUrl;
    String status;
    String role;
    LocalDate dob;
    Boolean nationalIdVerified;
    Boolean gender;

    public static UserProfileResponse fromUser(User user) {
        String fullAddress = buildFullAddress(user);

        return UserProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .nationalId(user.getNationalId())
                .phone(user.getPhone())
                .address(user.getAddress())
                .provinceCode(user.getProvinceCode())
                .provinceName(user.getProvinceName())
                .wardCode(user.getWardCode())
                .wardName(user.getWardName())
                .specificAddress(user.getSpecificAddress())
                .fullAddress(fullAddress)
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .role(user.getRole() != null ? user.getRole().getName() : "USER")
                .dob(user.getDob())
                .nationalIdVerified(user.getNationalIdVerified() != null ? user.getNationalIdVerified() : false)
                .gender(user.getGender())
                .build();
    }

    private static String buildFullAddress(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getSpecificAddress() != null && !user.getSpecificAddress().isBlank()) {
            sb.append(user.getSpecificAddress().trim()).append(", ");
        }
        if (user.getWardName() != null && !user.getWardName().isBlank()) {
            sb.append(user.getWardName().trim()).append(", ");
        }
        if (user.getProvinceName() != null && !user.getProvinceName().isBlank()) {
            sb.append(user.getProvinceName().trim());
        }
        String result = sb.toString().replaceAll(", $", "").trim();
        return result.isEmpty() ? null : result;
    }
}