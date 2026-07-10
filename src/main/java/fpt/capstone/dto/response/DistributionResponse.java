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
public class DistributionResponse {
    int id;
    Integer allocationPlanId;
    String planCode;
    Integer goodsInventoryId;
    String itemName;
    Integer benificiaryId;
    String benificiaryName;
    int quantity;
    LocalDate transferDate;
    String confirmationStatus;
    String notes;
    LocalDate createAt;
}