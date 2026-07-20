package fpt.capstone.dto.request;

import fpt.capstone.enums.SponsorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of POST/PUT /sponsors. Bean-validation messages are ErrorCode enum names
 * so {@code GlobalExceptionHandler} maps them to the business code/message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorRequest {

    @NotBlank(message = "ARGUMENT_INVALID")
    @Size(max = 255, message = "ARGUMENT_INVALID")
    private String name;

    @NotNull(message = "ARGUMENT_INVALID")
    private SponsorType type;

    @Size(max = 50, message = "ARGUMENT_INVALID")
    private String orgCode;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String contactPerson;

    @NotBlank(message = "ARGUMENT_INVALID")
    @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "ARGUMENT_INVALID")
    private String phone;

    @NotBlank(message = "ARGUMENT_INVALID")
    @Email(message = "ARGUMENT_INVALID")
    private String email;

    @NotBlank(message = "ARGUMENT_INVALID")
    private String address;

    private String note;
}
