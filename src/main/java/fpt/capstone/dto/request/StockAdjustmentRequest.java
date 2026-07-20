package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body (JSON part "data") of POST /inventory/{itemId}/adjustments. deltaQty must be non-zero. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "ARGUMENT_INVALID")
    private Integer deltaQty;

    @NotBlank(message = "REASON_REQUIRED")
    private String reason;
}
