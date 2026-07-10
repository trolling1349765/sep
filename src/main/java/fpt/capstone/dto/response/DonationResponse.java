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
public class DonationResponse {
    int id;
    String sponsorId;
    String sponsorName;
    Double amount;
    LocalDate transferDate;
    String purpose;
    String paymentMethod;
    String receiptStatus;
    String evidenceUrl;
    String notes;
    LocalDate createAt;
}