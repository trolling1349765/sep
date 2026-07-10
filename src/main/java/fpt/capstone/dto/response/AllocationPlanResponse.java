package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllocationPlanResponse {
    int id;
    String planCode;
    String title;
    String description;
    String status;
    LocalDate plannedDate;
    LocalDate completedDate;
    String notes;
    LocalDate createAt;
    LocalDate updateAt;
}