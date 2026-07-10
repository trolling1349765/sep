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
public class FundAllocationResponse {
    int id;
    Integer donationId;
    String purpose;
    Double amount;
    String beneficiaryType;
    String beneficiaryId;
    String beneficiaryName;
    LocalDate allocationDate;
    String notes;
}