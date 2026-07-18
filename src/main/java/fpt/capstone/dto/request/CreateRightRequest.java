package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRightRequest {

    @NotBlank(message = "ARGUMENT_INVALID")
    private String code;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String name;

    private String description;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String module;

    // Only required when introducing a brand-new module
    private String moduleName;
}
