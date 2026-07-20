package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Body (JSON part "data") of POST /distributions — issues one plan line. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionCreateRequest {

    @NotBlank(message = "ARGUMENT_INVALID")
    private String planLineId;

    @NotNull(message = "ARGUMENT_INVALID")
    @Positive(message = "ARGUMENT_INVALID")
    private Integer actualQty;

    @NotNull(message = "ARGUMENT_INVALID")
    private LocalDate issueDate;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String recipientName;

    private String recipientRelationship;

    private String note;
}
