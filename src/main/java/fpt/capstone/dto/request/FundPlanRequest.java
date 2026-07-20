package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body (JSON part "data") of POST/PUT /fund-plans. Exactly one of
 * {beneficiaryId, programName} must be set — enforced in the service (ARGUMENT_INVALID).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundPlanRequest {

    @NotNull(message = "ARGUMENT_INVALID")
    private Integer donationId;

    /** Exactly one of beneficiaryId / programName. */
    private Integer beneficiaryId;
    private String programName;

    @NotNull(message = "ARGUMENT_INVALID")
    @Positive(message = "ARGUMENT_INVALID")
    private BigDecimal amount;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String purpose;

    @NotNull(message = "ARGUMENT_INVALID")
    private LocalDate expectedDate;

    private String deliveryPlace;
}
