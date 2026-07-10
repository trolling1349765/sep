package fpt.capstone.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DonationRequest {
    String sponsorId;

    @Positive(message = "Số tiền phải lớn hơn 0")
    Double amount;

    LocalDate transferDate;

    String purpose;

    String paymentMethod;

    String receiptStatus;

    String evidenceUrl;

    String notes;
}