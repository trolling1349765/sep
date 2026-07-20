package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One beneficiary line of an item allocation plan. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPlanLineRequest {

    @NotNull(message = "ARGUMENT_INVALID")
    private Integer beneficiaryId;

    @NotNull(message = "ARGUMENT_INVALID")
    @Positive(message = "ARGUMENT_INVALID")
    private Integer plannedQty;
}
