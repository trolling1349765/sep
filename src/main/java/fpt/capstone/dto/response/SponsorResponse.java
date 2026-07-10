package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SponsorResponse {
    String id;
    String name;
    String sponsorType;
    String contactInfo;
    String phone;
    String email;
    String address;
    String representative;
    String taxCode;
    String status;
    LocalDate createAt;
    LocalDate updateAt;
}