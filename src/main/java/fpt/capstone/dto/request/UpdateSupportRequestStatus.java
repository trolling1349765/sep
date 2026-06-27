package fpt.capstone.dto.request;

import fpt.capstone.enums.SupportRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSupportRequestStatus {

    @NotNull(message = "Status is required")
    private SupportRequestStatus status;
}