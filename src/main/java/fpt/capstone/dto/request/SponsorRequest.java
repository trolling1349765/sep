package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SponsorRequest {
    @NotBlank(message = "Tên nhà tài trợ không được để trống")
    String name;

    String sponsorType;

    String contactInfo;

    String phone;

    String email;

    String address;

    String representative;

    String taxCode;

    String status;
}