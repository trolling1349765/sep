package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared body for reject / cancel / delete actions across the plan state machines
 * (fund plans and item allocation plans). A blank reason maps to REASON_REQUIRED.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonRequest {

    @NotBlank(message = "REASON_REQUIRED")
    private String reason;
}
