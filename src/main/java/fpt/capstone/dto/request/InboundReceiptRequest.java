package fpt.capstone.dto.request;

import fpt.capstone.enums.ItemCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Body (JSON part "data") of POST/PUT /inbound-receipts. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundReceiptRequest {

    @NotBlank(message = "ARGUMENT_INVALID")
    private String itemId;

    private String sponsorId;

    private String delivererName;

    @NotNull(message = "ARGUMENT_INVALID")
    @Positive(message = "ARGUMENT_INVALID")
    private Integer quantity;

    @NotNull(message = "ARGUMENT_INVALID")
    private ItemCondition condition;

    @NotNull(message = "ARGUMENT_INVALID")
    @PastOrPresent(message = "ARGUMENT_INVALID")
    private LocalDate receiveDate;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String evidenceName;
}
