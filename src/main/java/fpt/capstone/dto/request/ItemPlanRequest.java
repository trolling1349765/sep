package fpt.capstone.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Body (JSON part "data") of POST/PUT /item-plans — header + n lines.
 * plannedQty must equal the sum of the lines (checked in the service: LINE_SUM_MISMATCH).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPlanRequest {

    @NotBlank(message = "ARGUMENT_INVALID")
    private String itemId;

    @NotNull(message = "ARGUMENT_INVALID")
    @Positive(message = "ARGUMENT_INVALID")
    private Integer plannedQty;

    @NotNull(message = "ARGUMENT_INVALID")
    private LocalDate expectedDate;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String deliveryPlace;

    private String deliveryTimeWindow;

    @NotEmpty(message = "ARGUMENT_INVALID")
    @Valid
    private List<ItemPlanLineRequest> lines;
}
