package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of POST /support-items — the "+ Tạo vật phẩm mới" quick-create in the receipt form. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportItemRequest {

    @NotBlank(message = "ARGUMENT_INVALID")
    private String name;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String unit;

    private String description;
}
