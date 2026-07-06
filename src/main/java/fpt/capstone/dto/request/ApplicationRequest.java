package fpt.capstone.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationRequest {
    Integer id;
    String approvedBy;
    LocalDate approvedDate;
    int policyId;
    LocalDate submitDate;
    String status;
    String formType;
    LocalDate createdAt;
    String createdBy;
    LocalDate updatedAt;
    String updatedBy;
    boolean isDeleted;
}
