package fpt.capstone.dto.request;

import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DistributionRequest {
    Integer allocationPlanId;

    Integer goodsInventoryId;

    Integer benificiaryId;

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    int quantity;

    LocalDate transferDate;

    String notes;
}