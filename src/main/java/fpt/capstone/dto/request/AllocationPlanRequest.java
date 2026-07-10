package fpt.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllocationPlanRequest {
    @NotBlank(message = "Mã kế hoạch không được để trống")
    String planCode;

    @NotBlank(message = "Tiêu đề không được để trống")
    String title;

    String description;

    String status;

    LocalDate plannedDate;

    String notes;
}