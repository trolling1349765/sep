package fpt.capstone.dto.request;

import fpt.capstone.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body (JSON part "data") of POST/PUT /fundings. The TRANSFER-requires-transactionRef
 * rule is enforced in the service (TRANSACTION_REF_REQUIRED) since it is cross-field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingCreateRequest {

    @NotBlank(message = "ARGUMENT_INVALID")
    private String name;

    /** Optional sponsor link; when present must reference an ACTIVE sponsor. */
    private String sponsorId;

    @NotNull(message = "ARGUMENT_INVALID")
    @Positive(message = "ARGUMENT_INVALID")
    private BigDecimal amount;

    @NotNull(message = "ARGUMENT_INVALID")
    @PastOrPresent(message = "ARGUMENT_INVALID")
    private LocalDate receivedDate;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String purpose;

    @NotNull(message = "ARGUMENT_INVALID")
    private PaymentMethod paymentMethod;

    private String transactionRef;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String evidenceName;
}
